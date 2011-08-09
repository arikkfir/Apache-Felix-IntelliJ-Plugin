/*
 * Copyright (c) 2011 Arik Kfir. All rights reserved.
 */

package com.infolinks.idea.plugins.felix.bundle.internal;

/**
 * @author arik
 */
public class BadCommandException extends BundleServerException {

    public BadCommandException( String command ) {
        super( "Bad command: " + command );
    }
}
