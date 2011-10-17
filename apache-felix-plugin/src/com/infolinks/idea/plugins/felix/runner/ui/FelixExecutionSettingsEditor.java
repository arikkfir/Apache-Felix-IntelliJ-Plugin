package com.infolinks.idea.plugins.felix.runner.ui;

import com.infolinks.idea.plugins.felix.runner.FelixRunConfiguration;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.util.xml.ui.BigStringComponent;
import java.io.File;
import javax.swing.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.fileChooser.FileChooserDescriptorFactory.createSingleFolderDescriptor;

/**
 * @author arik
 */
public class FelixExecutionSettingsEditor extends SettingsEditor<FelixRunConfiguration> {

    private JPanel root;

    private TextFieldWithBrowseButton workDir;

    private TextFieldWithBrowseButton bundlesDir;

    private BigStringComponent jvmArgs;

    private JComboBox loggingLevel;

    private TextFieldWithBrowseButton felixConfigFile;

    private TextFieldWithBrowseButton felixSystemFile;

    public FelixExecutionSettingsEditor() {
        this.workDir.addBrowseFolderListener( "Select working directory", null, null, createSingleFolderDescriptor() );
        this.bundlesDir.addBrowseFolderListener( "Select bundles directory", null, null, createSingleFolderDescriptor() );
        this.felixConfigFile.addBrowseFolderListener( "Select Felix configuration file", null, null, new FileChooserDescriptor( true, false, false, false, false, false ) );
        this.felixSystemFile.addBrowseFolderListener( "Select Felix configuration file", null, null, new FileChooserDescriptor( true, false, false, false, false, false ) );
    }

    @Override
    protected void resetEditorFrom( FelixRunConfiguration configuration ) {
        this.workDir.setText( getFilePath( configuration.getWorkingDirectory() ) );
        this.jvmArgs.setText( configuration.getVmParameters() );
        this.bundlesDir.setText( getFilePath( configuration.getBundlesDirectory() ) );
        this.loggingLevel.setSelectedIndex( configuration.getFelixLogLevel() );
        this.felixConfigFile.setText( getFilePath( configuration.getFelixConfigFile() ) );
        this.felixSystemFile.setText( getFilePath( configuration.getFelixSystemFile() ) );
    }

    @Override
    protected void applyEditorTo( FelixRunConfiguration configuration ) throws ConfigurationException {
        configuration.setWorkingDirectory( createFile( this.workDir ) );
        configuration.setVmParameters( this.jvmArgs.getText() );
        configuration.setBundlesDirectory( createFile( this.bundlesDir ) );
        configuration.setFelixLogLevel( this.loggingLevel.getSelectedIndex() );
        configuration.setFelixConfigFile( createFile( this.felixConfigFile ) );
        configuration.setFelixSystemFile( createFile( this.felixSystemFile ) );
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
        this.jvmArgs = new BigStringComponent( "Enter JVM arguments" );
    }
}
