package com.infolinks.idea.plugins.felix.runner;

/**
 * @author arik
 */
public class DeploymentException extends Exception {

    @SuppressWarnings( { "SameParameterValue" } )
    public DeploymentException( String message ) {
        super( message );
    }

    public DeploymentException( String message, Throwable cause ) {
        super( message, cause );
    }
}
