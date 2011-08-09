package com.infolinks.idea.plugins.felix.runner.deploy;

import com.intellij.openapi.module.Module;

/**
 * @author arik
 */
public interface ModuleDeploymentInfo extends BundleDeploymentInfo {

    String getModuleName();

    Module getModule();
}
