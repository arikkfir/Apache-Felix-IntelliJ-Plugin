/*
 * Copyright (c) 2011 Arik Kfir. All rights reserved.
 */

package com.infolinks.idea.plugins.felix.bundle.internal;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author arik
 */
public class Listener {

    public static int PORT = 14078;

    private Acceptor acceptor;

    private Thread acceptorThread;

    public void stop() throws InterruptedException {
        if( this.acceptor != null ) {
            this.acceptor.stop = true;
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
        this.acceptor = new Acceptor();
        this.acceptorThread = new Thread( this.acceptor, "IntelliJ IDEA Acceptor thread" );
        this.acceptorThread.start();
    }

    class Acceptor implements Runnable {

        private boolean stop;

        @Override
        public void run() {
            try {
                ServerSocket listener = new ServerSocket( PORT, 10, InetAddress.getLocalHost() );
                listener.setReuseAddress( true );

                this.stop = false;
                while( !this.stop ) {
                    new Thread( new Connection( listener.accept() ) ).start();
                }

            } catch( IOException ioe ) {
                ioe.printStackTrace();
            }
        }
    }

    class Connection implements Runnable {

        private Socket server;

        private String line, input;

        Connection( Socket server ) {
            this.server = server;
        }

        public void run() {
            input = "";
            try {
                // Get input from the client
                DataInputStream in = new DataInputStream( server.getInputStream() );
                PrintStream out = new PrintStream( server.getOutputStream() );

                while( ( line = in.readLine() ) != null && !line.equals( "." ) ) {
                    input = input + line;
                    out.println( "I got:" + line );
                }

                // Now write to the client

                System.out.println( "Overall message is:" + input );
                out.println( "Overall message is:" + input );

                server.close();
            } catch( IOException ioe ) {
                System.out.println( "IOException on socket listen: " + ioe );
                ioe.printStackTrace();
            }
        }
    }
}

