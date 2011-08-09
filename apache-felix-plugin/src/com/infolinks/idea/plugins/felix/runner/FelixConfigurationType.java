package com.infolinks.idea.plugins.felix.runner;

import com.infolinks.idea.plugins.felix.util.ui.Icons;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;

/**
 * @author arik
 */
public class FelixConfigurationType extends ConfigurationTypeBase {

    public FelixConfigurationType() {
        super( "FelixConfigurationType", "Apache Felix", "Run an Apache Felix OSGi container", Icons.APACHE_FELIX );
        addFactory( new FelixConfigurationFactory() );
    }

    private RunConfiguration createTemplateConfiguration( ConfigurationFactory factory, Project project ) {
        return new FelixRunConfiguration( project, factory );
    }

    private class FelixConfigurationFactory extends ConfigurationFactory {

        private FelixConfigurationFactory() {
            super( FelixConfigurationType.this );
        }

        @Override
        public RunConfiguration createTemplateConfiguration( Project project ) {
            return FelixConfigurationType.this.createTemplateConfiguration( this, project );
        }
    }
}
