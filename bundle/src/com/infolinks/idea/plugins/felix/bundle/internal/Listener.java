/*
 * Copyright (c) 2011 Arik Kfir. All rights reserved.
 */

package com.infolinks.idea.plugins.felix.bundle.internal;

import com.infolinks.idea.plugins.felix.bundle.internal.util.IoUtils;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

/**
 * @author arik
 */
public class Listener {

    private BundleContext bundleContext;

    private int port = 14078;

    private Acceptor acceptor;

    private Thread acceptorThread;

    public void setBundleContext( BundleContext bundleContext ) {
        this.bundleContext = bundleContext;
    }

    public void setPort( int port ) {
        this.port = port;
    }

    public void stop() throws InterruptedException {
        if( this.acceptor != null ) {
            this.acceptor.stop();
            Thread.sleep( 1000 );
        }
        if( this.acceptorThread != null ) {
            this.acceptorThread.interrupt();
        }
        this.acceptor = null;
        this.acceptorThread = null;
    }

    public void start() throws IOException, InterruptedException {
        stop();
        this.acceptor = new Acceptor( this.bundleContext, this.port );
        this.acceptorThread = new Thread( this.acceptor, "IntelliJ IDEA Acceptor thread" );
        this.acceptorThread.start();
    }

    private static class Acceptor implements Runnable {

        private final BundleContext bundleContext;

        private final int port;

        private boolean stop;

        private Acceptor( BundleContext bundleContext, int port ) {
            this.bundleContext = bundleContext;
            this.port = port;
        }

        public void stop() {
            this.stop = true;
        }

        @Override
        public void run() {
            System.out.printf( "Starting Bundle Server listener thread...\n" );
            try {
                ServerSocket listener = new ServerSocket( this.port, 10 );
                listener.setReuseAddress( true );

                this.stop = false;
                while( !this.stop ) {
                    new Thread( new Connection( this.bundleContext, listener.accept() ) ).start();
                }
                System.out.printf( "Stopped Bundle Server listener thread...\n" );

            } catch( Exception e ) {
                System.out.printf( "The Bundle Server listener thread has been stopped due to exception: %s\n", e.getMessage() );
                e.printStackTrace();
            }
        }
    }

    private static class Connection implements Runnable {

        private static final Pattern COMMAND_PATTERN = Pattern.compile( "INSTALL:([^:]+)|UPDATE:([^:]+):([^:]+)|UNINSTALL:([^:]+):([^:]+)" );

        private final BundleContext bundleContext;

        private final Socket socket;

        private Connection( BundleContext bundleContext, Socket socket ) {
            this.bundleContext = bundleContext;
            this.socket = socket;
        }

        public void run() {
            BufferedReader reader = null;
            Writer writer = null;
            try {
                reader = new BufferedReader( new InputStreamReader( this.socket.getInputStream(), "UTF-8" ), 8096 );
                writer = new OutputStreamWriter( this.socket.getOutputStream(), "UTF-8" );

                String line = reader.readLine();
                if( line != null ) {
                    Matcher matcher = COMMAND_PATTERN.matcher( line );
                    if( !matcher.matches() ) {
                        throw new BadCommandException( line );
                    }

                    System.out.println( "Bundle server - read line: " + line );
                    if( line.startsWith( "INSTALL:" ) ) {
                        try {
                            this.bundleContext.installBundle( matcher.group( 1 ) );
                        } catch( BundleException e ) {
                            throw new BundleServerException( "Bundle install error: " + e.getMessage(), e );
                        }
                    } else if( line.startsWith( "UPDATE:" ) ) {
                        try {
                            findBundle( matcher.group( 2 ), getVersion( matcher.group( 3 ) ) ).update();
                        } catch( BundleException e ) {
                            throw new BundleServerException( "Bundle update error: " + e.getMessage(), e );
                        }
                    } else if( line.startsWith( "UNINSTALL:" ) ) {
                        try {
                            findBundle( matcher.group( 4 ), getVersion( matcher.group( 5 ) ) ).uninstall();
                        } catch( BundleException e ) {
                            throw new BundleServerException( "Bundle uninstall error: " + e.getMessage(), e );
                        } catch( IllegalArgumentException e ) {
                            throw new BundleServerException( "Bundle uninstall error: " + e.getMessage(), e );
                        }
                    } else {
                        throw new BadCommandException( "Illegal bundle command: " + line );
                    }
                }
                writer.write( "OK\n" );

            } catch( BundleServerException e ) {
                printKnownError( writer, e );

            } catch( Exception e ) {
                printUnknownError( writer, e );

            } finally {
                IoUtils.closeQuietly( reader );
                if( writer != null ) {
                    try {
                        writer.flush();
                    } catch( IOException ignore ) {
                        //no-op
                    }
                }
                IoUtils.closeQuietly( writer );
                IoUtils.closeQuietly( this.socket );
            }
        }

        private Command getCommand( String commandName ) throws BadCommandException {
            try {
                return Command.valueOf( commandName );
            } catch( IllegalArgumentException e ) {
                throw new BadCommandException( "Unknown bundle command: " + commandName );
            }
        }

        private Version getVersion( String versionValue ) throws BadCommandException {
            try {
                return new Version( versionValue );
            } catch( Exception e ) {
                throw new BadCommandException( "Illegal bundle version: " + versionValue );
            }
        }

        private Bundle findBundle( String symbolicName, Version version ) throws BadCommandException {
            for( Bundle bundle : this.bundleContext.getBundles() ) {
                String currentSymbolicName = bundle.getSymbolicName();
                if( currentSymbolicName == null || !currentSymbolicName.equals( symbolicName ) ) {
                    continue;
                }
                Version currentVersion = bundle.getVersion();
                if( currentVersion == null || !currentVersion.equals( version ) ) {
                    continue;
                }
                return bundle;
            }
            throw new BadCommandException( "Bundle '" + symbolicName + "-" + version + "' could not be found" );
        }

        private void printKnownError( Writer out, Exception e ) {
            printKnownError( out, e.getMessage() );
        }

        private void printKnownError( Writer out, String message ) {
            if( out != null ) {
                try {
                    out.write( "ERR:" + message + "\n" );
                } catch( IOException e1 ) {
                    e1.printStackTrace();
                }
            } else {
                System.out.println( "ERR:" + message );
            }
        }

        private void printUnknownError( Writer out, Exception e ) {
            e.printStackTrace();
            printKnownError( out, e );
        }
    }

    /*
        public static void main( String[] args ) {
            printMatch( "INSTALL" );
            printMatch( "INSTALL:" );
            printMatch( "INSTALL:arik" );
            printMatch( "INSTALL:arik:" );
            printMatch( "INSTALL:arik:1.1.1" );
            printMatch( "UPDATE" );
            printMatch( "UPDATE:" );
            printMatch( "UPDATE:bsn" );
            printMatch( "UPDATE:bsn:" );
            printMatch( "UPDATE:bsn:ver" );
            printMatch( "UPDATE:bsn:ver:" );
            printMatch( "UPDATE:bsn:ver:extra" );
            printMatch( "UNINSTALL" );
            printMatch( "UNINSTALL:" );
            printMatch( "UNINSTALL:bsn" );
            printMatch( "UNINSTALL:bsn:" );
            printMatch( "UNINSTALL:bsn:ver" );
            printMatch( "UNINSTALL:bsn:ver:" );
            printMatch( "UNINSTALL:bsn:ver:extra" );
        }

        private static void printMatch( String input ) {
            System.out.println( "Input:   " + input );

            Matcher matcher = Pattern.compile( "INSTALL:([^:]+)|UPDATE:([^:]+):([^:]+)|UNINSTALL:([^:]+):([^:]+)" ).matcher( input );
            System.out.println( "Matches: " + matcher.matches() );

            if( matcher.matches() ) {
                System.out.println( "Groups: " );
                for( int i = 1; i <= matcher.groupCount(); i++ ) {
                    System.out.println( "    " + i + ": " + matcher.group( i ) );
                }
            }
            System.out.println();
            System.out.println();
        }
    */
}

