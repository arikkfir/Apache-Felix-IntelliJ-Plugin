package com.infolinks.idea.plugins.felix.runner;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.Icons;

import static com.intellij.openapi.ui.Messages.showErrorDialog;

/**
 * @author arik
 */
@SuppressWarnings( { "ComponentNotRegistered" } )
public class RedeployHotDeployBundlesAction extends AnAction {

    private final FelixRunConfiguration runConfiguration;

    public RedeployHotDeployBundlesAction( FelixRunConfiguration runConfiguration ) {
        super( "Redeploy bundles", "Redeploy bundles in hot-deploy directory", Icons.SYNCHRONIZE_ICON );
        this.runConfiguration = runConfiguration;
    }

    @Override
    public void update( AnActionEvent e ) {
        e.getPresentation().setEnabled( this.runConfiguration.isEnableHotDeploy() );
    }

    @Override
    public void actionPerformed( final AnActionEvent event ) {
        makeProject( event.getData( DataKeys.PROJECT ), event.getData( DataKeys.CONSOLE_VIEW ) );
    }

    private void makeProject( Project project, final ConsoleView consoleView ) {
        final CompilerManager manager = CompilerManager.getInstance( project );
        final CompileScope compileScope = manager.createProjectCompileScope( project );

        if( manager.isUpToDate( compileScope ) ) {

            //
            // project compilation is up to date - can simply run post-action
            //
            ApplicationManager.getApplication().invokeLater( new Runnable() {

                @Override
                public void run() {
                    deploy( consoleView );
                }
            } );

        } else {

            //
            // make is needed
            //
            ApplicationManager.getApplication().invokeLater( new Runnable() {

                public void run() {
                    manager.make( compileScope, new CompileStatusNotification() {

                        public void finished( boolean aborted,
                                              int errors,
                                              int warnings,
                                              CompileContext compileContext ) {
                            if( !aborted && errors == 0 ) {
                                deploy( consoleView );
                            }
                        }
                    } );
                }
            } );
        }
    }

    @SuppressWarnings( { "UnusedParameters" } )
    private void deploy( ConsoleView consoleView ) {
        try {
            FelixBundlesDeployer.getInstance( runConfiguration.getProject() ).deploySelectedBundles( runConfiguration );
        } catch( DeploymentException e ) {
            if( consoleView != null ) {
                consoleView.print( e.getMessage(), ConsoleViewContentType.ERROR_OUTPUT );
            } else {
                showErrorDialog( e.getMessage(), "Deployment error" );
            }
        }
    }
}
