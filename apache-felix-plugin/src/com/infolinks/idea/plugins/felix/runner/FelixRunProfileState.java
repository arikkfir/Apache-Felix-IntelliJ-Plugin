package com.infolinks.idea.plugins.felix.runner;

import com.infolinks.idea.plugins.felix.build.BundleCompiler;
import com.infolinks.idea.plugins.felix.framework.FelixFrameworkManager;
import com.infolinks.idea.plugins.felix.runner.deploy.BundleDeploymentInfo;
import com.infolinks.idea.plugins.felix.runner.deploy.ModuleDeploymentInfo;
import com.intellij.execution.CantRunException;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.JavaCommandLineState;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.codehaus.plexus.util.FileUtils;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.ui.Messages.showErrorDialog;
import static java.util.Arrays.asList;

/**
 * @author arik
 */
public class FelixRunProfileState extends JavaCommandLineState {

    private final Logger logger = Logger.getInstance( getClass().getName() );

    private final Project project;

    private final FelixRunConfiguration runConfiguration;

    public FelixRunProfileState( FelixRunConfiguration felixRunConfiguration,
                                 @NotNull ExecutionEnvironment environment ) {
        super( environment );
        this.runConfiguration = felixRunConfiguration;
        this.project = environment.getProject();
    }

    @Override
    protected AnAction[] createActions( ConsoleView console, ProcessHandler processHandler, Executor executor ) {
        List<AnAction> actions = new ArrayList<AnAction>( asList( super.createActions( console, processHandler, executor ) ) );
        if( console != null ) {
            //TODO: actions.add( new RedeployHotDeployBundlesAction( this.runConfiguration ) );
        }
        return actions.toArray( new AnAction[ actions.size() ] );
    }

    @NotNull
    @Override
    protected OSProcessHandler startProcess() throws ExecutionException {
        File frameworkPath = FelixFrameworkManager.getInstance().getFrameworkPath();
        if( frameworkPath == null || !frameworkPath.isDirectory() ) {
            throw new CantRunException( "Felix Framework path is not configured properly (see Settings / Apache Felix)" );
        }

        try {
            FelixBundlesDeployer.getInstance( this.project ).setupDeployment( this.runConfiguration );
            OSProcessHandler processHandler = super.startProcess();

            final CompilationStatusListener compilationStatusListener = new CompilationStatusListener() {

                @Override
                public void compilationFinished( boolean aborted,
                                                 int errors,
                                                 int warnings,
                                                 CompileContext compileContext ) {
                    if( !aborted && errors == 0 ) {
                        final Set<Module> modules = compileContext.getUserData( BundleCompiler.MODIFIED_MODULES_KEY );
                        try {
                            compileContext.getProgressIndicator().setText( "Redeploying compiled bundles to running Apache Felix '" + runConfiguration.getName() + "'" );

                            FelixBundlesDeployer.getInstance( project ).deploySelectedBundles( runConfiguration, new FelixBundlesDeployer.ModuleDeploymentFilter() {

                                @Override
                                public boolean shouldDeploy( BundleDeploymentInfo deploymentInfo ) {
                                    if( deploymentInfo instanceof ModuleDeploymentInfo ) {
                                        ModuleDeploymentInfo moduleDeploymentInfo = ( ModuleDeploymentInfo ) deploymentInfo;
                                        Module module = moduleDeploymentInfo.getModule();
                                        return modules != null && modules.contains( module );
                                    }
                                    return false;
                                }
                            } );
                        } catch( DeploymentException e ) {
                            //TODO arik (12/29/10): output to console view of running
                            showErrorDialog( e.getMessage(), "Deployment Error" );
                        }
                    }
                }
            };

            processHandler.addProcessListener( new ProcessAdapter() {

                @Override
                public void startNotified( ProcessEvent event ) {
                    CompilerManager.getInstance( project ).addCompilationStatusListener( compilationStatusListener );
                }

                @Override
                public void processTerminated( ProcessEvent event ) {
                    CompilerManager.getInstance( project ).removeCompilationStatusListener( compilationStatusListener );
                }
            } );

            return processHandler;

        } catch( DeploymentException e ) {
            this.logger.error( e.getMessage(), e );
            throw new CantRunException( e.getMessage() );
        }
    }

    @Override
    protected JavaParameters createJavaParameters() throws ExecutionException {
        JavaParameters params = createBaseJavaParameters();
        setupStandardFelix( params );

        String vmParameters = this.runConfiguration.getVmParameters();
        if( vmParameters != null && vmParameters.trim().length() > 0 ) {
            params.getVMParametersList().addParametersString( vmParameters.trim() );
        }

        return params;
    }

    private JavaParameters createBaseJavaParameters() {
        JavaParameters params = new JavaParameters();
        params.setJdk( ProjectRootManager.getInstance( this.project ).getProjectSdk() );
        params.setWorkingDirectory( this.runConfiguration.getWorkingDirectory() );
        params.getClassPath().add( new File( FelixFrameworkManager.getInstance().getFrameworkPath(), "bin/felix.jar" ) );
        return params;
    }

    private void setupStandardFelix( JavaParameters params ) throws ExecutionException {
        params.setMainClass( "org.apache.felix.main.Main" );
        params.getVMParametersList().defineProperty( "felix.auto.deploy.dir", noWinSlashes( this.runConfiguration.getBundlesDirectory() ) );
        params.getVMParametersList().defineProperty( "felix.auto.deploy.action", "install,start" );
        params.getVMParametersList().defineProperty( "felix.log.level", this.runConfiguration.getFelixLogLevel() + "" );
        params.getVMParametersList().defineProperty( "felix.startlevel.bundle", "1" );
        params.getVMParametersList().defineProperty( "org.osgi.framework.startlevel.beginning", "1" );

        if( this.runConfiguration.getFelixSystemFile() != null ) {
            params.getVMParametersList().defineProperty( "felix.system.properties", this.runConfiguration.getFelixSystemFile().toURI().toString() );
        }
        if( this.runConfiguration.getFelixConfigFile() != null ) {
            params.getVMParametersList().defineProperty( "felix.config.properties", this.runConfiguration.getFelixConfigFile().toURI().toString() );
        }

        File workDir = this.runConfiguration.getWorkingDirectory();
        if( !workDir.exists() && !workDir.mkdirs() ) {
            throw new ExecutionException( "Could not create working directory: " + workDir );
        }

        File felixCacheDir = new File( workDir, "felix-cache" );
        if( felixCacheDir.exists() ) {
            if( felixCacheDir.isDirectory() ) {
                try {
                    FileUtils.deleteDirectory( felixCacheDir );
                } catch( IOException e ) {
                    //no-op
                }
            } else if( felixCacheDir.isFile() ) {
                throw new ExecutionException( "Felix cache directory points to a file at: " + felixCacheDir );
            }
        }
    }

    private String noWinSlashes( File file ) {
        return file.getAbsolutePath().replace( '\\', '/' );
    }
}
