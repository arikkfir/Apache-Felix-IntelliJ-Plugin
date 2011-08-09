/*
 * Copyright (c) 2011 Arik Kfir. All rights reserved.
 */

package com.infolinks.idea.plugins.felix.bundle.internal.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;

/**
 * @author arik
 */
public class IoUtils {

    public static void closeQuietly( Socket socket ) {
        if( socket != null ) {
            try {
                socket.close();
            } catch( Exception ignore ) {
                //no-op
            }
        }
    }

    public static void closeQuietly( InputStream inputStream ) {
        if( inputStream != null ) {
            try {
                inputStream.close();
            } catch( Exception ignore ) {
                //no-op
            }
        }
    }

    public static void closeQuietly( Reader reader ) {
        if( reader != null ) {
            try {
                reader.close();
            } catch( Exception ignore ) {
                //no-op
            }
        }
    }

    public static void closeQuietly( OutputStream outputStream ) {
        if( outputStream != null ) {
            try {
                outputStream.close();
            } catch( Exception ignore ) {
                //no-op
            }
        }
    }

    public static void closeQuietly( Writer writer ) {
        if( writer != null ) {
            try {
                writer.close();
            } catch( Exception ignore ) {
                //no-op
            }
        }
    }
}
