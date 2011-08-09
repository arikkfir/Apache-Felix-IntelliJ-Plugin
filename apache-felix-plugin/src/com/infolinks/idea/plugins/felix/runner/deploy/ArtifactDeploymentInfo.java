package com.infolinks.idea.plugins.felix.runner.deploy;

/**
 * @author arik
 */
public interface ArtifactDeploymentInfo extends BundleDeploymentInfo {

    String getGroupId();

    String getArtifactId();

    String getMavenVersion();

}
