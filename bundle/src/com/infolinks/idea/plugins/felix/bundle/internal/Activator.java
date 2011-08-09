/*
 * Copyright (c) 2011 Arik Kfir. All rights reserved.
 */

package com.infolinks.idea.plugins.felix.bundle.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author arik
 */
public class Activator implements BundleActivator {

    private Listener listener;

    @Override
    public void start( BundleContext context ) throws Exception {
        this.listener = new Listener();
        this.listener.start();
    }

    @Override
    public void stop( BundleContext context ) throws Exception {
        this.listener.stop();
    }
}
