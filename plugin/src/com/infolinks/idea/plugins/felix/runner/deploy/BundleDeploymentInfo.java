package com.infolinks.idea.plugins.felix.runner.deploy;

import com.infolinks.idea.plugins.felix.runner.bundle.BundleInfo;

import java.io.File;

/**
 * @author arik
 */
public interface BundleDeploymentInfo extends BundleInfo {

    boolean isValid();

    boolean getIncludeVersionInFilename();

    void setIncludeVersionInFilename( boolean value );

    File getFile();

    String getDeployFilename();

    File getDeployDir();

    void setDeployDir( File deployDir );
}
