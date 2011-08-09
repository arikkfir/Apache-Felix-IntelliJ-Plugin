package com.infolinks.idea.plugins.felix.runner.deploy;

/**
 * @author arik
 */
public enum DeploymentMethod {

    BUNDLES( "Startup" ),
    HOT_DEPLOY( "Hot-deploy" );

    private final String label;

    private DeploymentMethod( String label ) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
