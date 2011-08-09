package com.infolinks.idea.plugins.felix.runner.ui;

import com.infolinks.idea.plugins.felix.runner.FelixRunConfiguration;

/**
 * @author arik
 */
public interface RunConfigurationAware {

    void setRunConfiguration( FelixRunConfiguration configuration );

}
