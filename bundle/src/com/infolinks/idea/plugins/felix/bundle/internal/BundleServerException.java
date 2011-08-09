/*
 * Copyright (c) 2011 Arik Kfir. All rights reserved.
 */

package com.infolinks.idea.plugins.felix.bundle.internal;

/**
 * @author arik
 */
public class BundleServerException extends Exception {

    public BundleServerException( String message ) {
        super( message );
    }

    public BundleServerException( String message, Throwable cause ) {
        super( message, cause );
    }
}
