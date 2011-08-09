package com.infolinks.idea.plugins.felix.runner.ui;

import com.infolinks.idea.plugins.felix.runner.FelixRunConfiguration;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.util.xml.ui.BigStringComponent;
import java.io.File;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.fileChooser.FileChooserDescriptorFactory.createSingleFolderDescriptor;

/**
 * @author arik
 */
public class FelixExecutionSettingsEditor extends SettingsEditor<FelixRunConfiguration> {

    private JPanel root;

    private TextFieldWithBrowseButton workDir;

    private TextFieldWithBrowseButton startupBundlesDir;

    private JCheckBox enableHotDeploy;

    private HotDeployDirectoriesPanel hotDeployDirs;

    private BigStringComponent jvmArgs;

    private JTextField polling;

    public FelixExecutionSettingsEditor() {
        this.workDir.addBrowseFolderListener( "Select working directory", null, null, createSingleFolderDescriptor() );
        this.startupBundlesDir.addBrowseFolderListener( "Select bundles directory", null, null, createSingleFolderDescriptor() );
        this.enableHotDeploy.addChangeListener( new ChangeListener() {

            @Override
            public void stateChanged( ChangeEvent e ) {
                hotDeployDirs.setEnabled( enableHotDeploy.isSelected() );
            }
        } );
    }

    @Override
    protected void resetEditorFrom( FelixRunConfiguration configuration ) {
        this.polling.setText( configuration.getPolling() + "" );
        this.workDir.setText( getFilePath( configuration.getWorkingDirectory() ) );
        this.startupBundlesDir.setText( getFilePath( configuration.getBundlesDirectory() ) );
        this.enableHotDeploy.setSelected( configuration.isEnableHotDeploy() );
        this.hotDeployDirs.setDirectories( configuration.getHotDeployDirectories() );
        this.hotDeployDirs.setProject( configuration.getProject() );
        this.jvmArgs.setText( configuration.getVmParameters() );
    }

    @Override
    protected void applyEditorTo( FelixRunConfiguration configuration ) throws ConfigurationException {
        configuration.setWorkingDirectory( createFile( this.workDir ) );
        configuration.setBundlesDirectory( createFile( this.startupBundlesDir ) );
        configuration.setEnableHotDeploy( this.enableHotDeploy.isSelected() );
        configuration.setHotDeployDirectories( this.hotDeployDirs.getDirectories() );
        configuration.setVmParameters( this.jvmArgs.getText() );
        try {
            configuration.setPolling( Integer.parseInt( this.polling.getText() ) );
        } catch( NumberFormatException e ) {
            //ignore
        }
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return this.root;
    }

    @Override
    protected void disposeEditor() {
        //no-op
    }

    private File createFile( TextFieldWithBrowseButton textField ) {
        return createFile( textField.getTextField() );
    }

    private File createFile( JTextField textField ) {
        if( textField == null || StringUtils.isBlank( textField.getText() ) ) {
            return null;
        } else {
            return new File( textField.getText() );
        }
    }

    private String getFilePath( File file ) {
        return file == null ? "" : file.getAbsolutePath();
    }

    private void createUIComponents() {
        this.hotDeployDirs = new HotDeployDirectoriesPanel();
        this.jvmArgs = new BigStringComponent( "Enter JVM arguments" );
    }
}
