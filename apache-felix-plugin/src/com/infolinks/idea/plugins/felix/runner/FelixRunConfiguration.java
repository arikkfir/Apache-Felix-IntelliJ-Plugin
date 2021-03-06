package com.infolinks.idea.plugins.felix.runner;

import com.infolinks.idea.plugins.felix.framework.FelixFrameworkManager;
import com.infolinks.idea.plugins.felix.runner.deploy.*;
import com.infolinks.idea.plugins.felix.runner.ui.FelixDeploymentSettingsEditor;
import com.infolinks.idea.plugins.felix.runner.ui.FelixExecutionSettingsEditor;
import com.intellij.diagnostic.logging.LogConfigurationPanel;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.codehaus.plexus.util.FileUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * @author arik
 */
public class FelixRunConfiguration extends RunConfigurationBase implements ModuleRunConfiguration {

    private static final Logger LOG = Logger.getInstance( FelixRunConfiguration.class.getName() );

    private static final int DEFAULT_FELIX_LOG_LEVEL = 3;

    private static final int DEFAULT_BUNDLE_SERVER_PORT = 14078;

    private int felixLogLevel = DEFAULT_FELIX_LOG_LEVEL;

    private File workingDirectory;

    private File bundlesDirectory;

    private File felixConfigFile;

    private File felixSystemFile;

    private int bundleServerPort = DEFAULT_BUNDLE_SERVER_PORT;

    private String vmParameters;

    private List<BundleDeploymentInfo> bundles = new ArrayList<BundleDeploymentInfo>();

    private int polling = 1000;

    public FelixRunConfiguration( Project project, ConfigurationFactory factory ) {
        super( project, factory, "Apache Felix" );
    }

    public int getPolling() {
        return polling;
    }

    public void setPolling( int polling ) {
        this.polling = polling;
    }

    public int getFelixLogLevel() {
        return this.felixLogLevel;
    }

    public void setFelixLogLevel( int felixLogLevel ) {
        this.felixLogLevel = felixLogLevel;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory( File workingDirectory ) {
        this.workingDirectory = workingDirectory;
    }

    public File getBundlesDirectory() {
        return bundlesDirectory;
    }

    public void setBundlesDirectory( File bundlesDirectory ) {
        this.bundlesDirectory = bundlesDirectory;
    }

    public File getFelixConfigFile() {
        return felixConfigFile;
    }

    public void setFelixConfigFile( File felixConfigFile ) {
        this.felixConfigFile = felixConfigFile;
    }

    public File getFelixSystemFile() {
        return felixSystemFile;
    }

    public void setFelixSystemFile( File felixSystemFile ) {
        this.felixSystemFile = felixSystemFile;
    }

    public int getBundleServerPort() {
        return bundleServerPort;
    }

    public void setBundleServerPort( int bundleServerPort ) {
        this.bundleServerPort = bundleServerPort;
    }

    public String getVmParameters() {
        return vmParameters;
    }

    public void setVmParameters( String vmParameters ) {
        this.vmParameters = vmParameters;
    }

    public List<BundleDeploymentInfo> getBundles() {
        List<BundleDeploymentInfo> bundles = new ArrayList<BundleDeploymentInfo>( this.bundles );
        Collections.sort( bundles, new BundleDeploymentInfoComparator() );
        return bundles;
    }

    public void setBundles( List<BundleDeploymentInfo> bundles ) {
        this.bundles = new ArrayList<BundleDeploymentInfo>( bundles );
    }

    @Override
    public SettingsEditor<FelixRunConfiguration> getConfigurationEditor() {
        SettingsEditorGroup<FelixRunConfiguration> settingsEditor = new SettingsEditorGroup<FelixRunConfiguration>();
        settingsEditor.addEditor( "Execution", new FelixExecutionSettingsEditor() );
        settingsEditor.addEditor( "Deployment", new FelixDeploymentSettingsEditor() );
        settingsEditor.addEditor( "Logs", new LogConfigurationPanel<FelixRunConfiguration>() );
        return settingsEditor;
    }

    @NotNull
    @Override
    public Module[] getModules() {
        List<Module> modules = new ArrayList<Module>();
        for( BundleDeploymentInfo bundle : this.bundles ) {
            if( bundle instanceof ModuleDeploymentInfo ) {
                Module module = ( ( ModuleDeploymentInfo ) bundle ).getModule();
                if( module != null ) {
                    modules.add( module );
                }
            }
        }
        return modules.toArray( new Module[ modules.size() ] );
    }

    @Override
    public RunProfileState getState( @NotNull Executor executor, @NotNull ExecutionEnvironment env )
            throws ExecutionException {
        FelixRunProfileState felixRunProfileState = new FelixRunProfileState( this, env );

        TextConsoleBuilderFactory tcbf = TextConsoleBuilderFactory.getInstance();
        felixRunProfileState.setConsoleBuilder( tcbf.createBuilder( env.getProject() ) );

        return felixRunProfileState;
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        File frameworkPath = FelixFrameworkManager.getInstance().getFrameworkPath();
        if( frameworkPath == null || !frameworkPath.isDirectory() ) {
            throw new RuntimeConfigurationError( "Felix framework path does not exist", new Runnable() {

                @Override
                public void run() {
                    ShowSettingsUtil.getInstance().showSettingsDialog( getProject(), FelixFrameworkManager.getInstance() );
                }
            } );
        }
        if( this.workingDirectory == null ) {
            throw new RuntimeConfigurationError( "Working directory is empty" );
        }
        if( !this.workingDirectory.isDirectory() ) {
            throw new RuntimeConfigurationWarning( "Working directory does not exist.", new Runnable() {

                @Override
                public void run() {
                    try {
                        FileUtils.forceMkdir( workingDirectory );
                    } catch( IOException e ) {
                        LOG.warn( "Could not create directory: " + workingDirectory, e );
                    }
                }
            } );
        }
        if( this.bundlesDirectory == null ) {
            throw new RuntimeConfigurationError( "Bundles directory is empty" );
        }
        if( !this.bundlesDirectory.isDirectory() ) {
            throw new RuntimeConfigurationWarning( "Bundles directory does not exist.", new Runnable() {

                @Override
                public void run() {
                    try {
                        FileUtils.forceMkdir( bundlesDirectory );
                    } catch( IOException e ) {
                        LOG.warn( "Could not create directory: " + bundlesDirectory, e );
                    }
                }
            } );
        }
        if( this.bundles.isEmpty() ) {
            throw new RuntimeConfigurationWarning( "No bundles were selected for deployment" );
        }
    }

    @SuppressWarnings( { "deprecation" } )
    @Override
    public SettingsEditor<JDOMExternalizable> getRunnerSettingsEditor( ProgramRunner runner ) {
        return null;
    }

    @SuppressWarnings( { "deprecation" } )
    @Override
    public JDOMExternalizable createRunnerSettings( ConfigurationInfoProvider provider ) {
        return null;
    }

    @Override
    public void readExternal( Element element ) throws InvalidDataException {
        super.readExternal( element );

        Element felixLoggingLevelElement = element.getChild( "logging-level" );
        if( felixLoggingLevelElement != null ) {
            String level = felixLoggingLevelElement.getAttributeValue( "value" );
            try {
                this.felixLogLevel = Integer.parseInt( level );
            } catch( NumberFormatException e ) {
                this.felixLogLevel = DEFAULT_FELIX_LOG_LEVEL;
            }
        } else {
            this.felixLogLevel = DEFAULT_FELIX_LOG_LEVEL;
        }

        Element bundleServerPort = element.getChild( "bundle-server-port" );
        if( bundleServerPort != null ) {
            String port = bundleServerPort.getAttributeValue( "value" );
            try {
                this.bundleServerPort = Integer.parseInt( port );
            } catch( NumberFormatException e ) {
                this.bundleServerPort = DEFAULT_BUNDLE_SERVER_PORT;
            }
        } else {
            this.bundleServerPort = DEFAULT_BUNDLE_SERVER_PORT;
        }

        Element workingDirectoryElement = element.getChild( "working-directory" );
        if( workingDirectoryElement != null ) {
            String path = workingDirectoryElement.getText();
            if( isNotBlank( path ) ) {
                this.workingDirectory = new File( path );
            }
        }

        Element bundlesDirectoryElement = element.getChild( "bundles-directory" );
        if( bundlesDirectoryElement != null ) {
            String path = bundlesDirectoryElement.getText();
            if( isNotBlank( path ) ) {
                this.bundlesDirectory = new File( path );
            }
        }

        Element felixConfigFileElement = element.getChild( "felix-config-file" );
        if( felixConfigFileElement != null ) {
            String path = felixConfigFileElement.getText();
            if( isNotBlank( path ) ) {
                this.felixConfigFile = new File( path );
            }
        }

        Element felixSystemFileElement = element.getChild( "felix-system-file" );
        if( felixSystemFileElement != null ) {
            String path = felixSystemFileElement.getText();
            if( isNotBlank( path ) ) {
                this.felixSystemFile = new File( path );
            }
        }

        Element vmParamsElement = element.getChild( "vm-parameters" );
        if( vmParamsElement != null ) {
            this.vmParameters = vmParamsElement.getText();
        }

        List<BundleDeploymentInfo> bundles = new ArrayList<BundleDeploymentInfo>();
        Element deploymentElement = element.getChild( "deployment" );
        if( deploymentElement != null ) {

            @SuppressWarnings( { "unchecked" } )
            List<Element> moduleElements = deploymentElement.getChildren( "module" );
            for( Element moduleElement : moduleElements ) {
                String moduleName = moduleElement.getAttributeValue( "name" );
                if( moduleName != null && moduleName.trim().length() > 0 ) {
                    bundles.add( new ModuleDeploymentInfoImpl( getProject(), moduleName ) );
                }
            }

            @SuppressWarnings( { "unchecked" } )
            List<Element> bundleElements = deploymentElement.getChildren( "artifact" );
            for( Element artifactElement : bundleElements ) {
                String groupId = artifactElement.getAttributeValue( "group-id" );
                String artifactId = artifactElement.getAttributeValue( "artifact-id" );
                String version = artifactElement.getAttributeValue( "version" );
                if( groupId != null && artifactId != null && version != null ) {
                    bundles.add( new ArtifactFileDeploymentInfoImpl( getProject(), groupId, artifactId, version ) );
                }
            }
        }
        setBundles( bundles );
    }

    @Override
    public void writeExternal( Element element ) throws WriteExternalException {
        super.writeExternal( element );

        //
        // logging level
        //
        Element felixLoggingLevelElement = new Element( "logging-level" );
        felixLoggingLevelElement.setAttribute( "value", this.felixLogLevel + "" );
        element.addContent( felixLoggingLevelElement );

        //
        // working dir
        //
        if( this.workingDirectory != null ) {
            Element directoryElement = new Element( "working-directory" );
            directoryElement.setText( this.workingDirectory.getAbsolutePath() );
            element.addContent( directoryElement );
        }

        //
        // bundles dir
        //
        if( this.bundlesDirectory != null ) {
            Element directoryElement = new Element( "bundles-directory" );
            directoryElement.setText( this.bundlesDirectory.getAbsolutePath() );
            element.addContent( directoryElement );
        }

        //
        // felix config file
        //
        if( this.felixConfigFile != null ) {
            Element fileElement = new Element( "felix-config-file" );
            fileElement.setText( this.felixConfigFile.getAbsolutePath() );
            element.addContent( fileElement );
        }

        //
        // felix config file
        //
        if( this.felixSystemFile != null ) {
            Element fileElement = new Element( "felix-system-file" );
            fileElement.setText( this.felixSystemFile.getAbsolutePath() );
            element.addContent( fileElement );
        }

        //
        // vm parameters
        //
        Element vmParamsElement = new Element( "vm-parameters" );
        vmParamsElement.setText( this.vmParameters );
        element.addContent( vmParamsElement );

        //
        // bundles / deployment
        //
        Element deploymentElement = new Element( "deployment" );
        for( BundleDeploymentInfo bundle : this.bundles ) {
            if( bundle instanceof ArtifactDeploymentInfo ) {
                ArtifactDeploymentInfo artifactInfo = ( ArtifactDeploymentInfo ) bundle;
                Element artifactInfoElement = new Element( "artifact" );
                artifactInfoElement.setAttribute( "group-id", artifactInfo.getGroupId() );
                artifactInfoElement.setAttribute( "artifact-id", artifactInfo.getArtifactId() );
                artifactInfoElement.setAttribute( "version", artifactInfo.getMavenVersion() );
                deploymentElement.addContent( artifactInfoElement );
            } else if( bundle instanceof ModuleDeploymentInfo ) {
                ModuleDeploymentInfo moduleInfo = ( ModuleDeploymentInfo ) bundle;
                Element moduleElement = new Element( "module" );
                moduleElement.setAttribute( "name", moduleInfo.getModuleName() );
                deploymentElement.addContent( moduleElement );
            }
        }
        element.addContent( deploymentElement );
    }
}
