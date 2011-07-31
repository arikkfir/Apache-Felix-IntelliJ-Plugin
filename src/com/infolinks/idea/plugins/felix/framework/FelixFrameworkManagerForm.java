package com.infolinks.idea.plugins.felix.framework;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import java.io.File;
import javax.swing.*;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * @author arik
 */
public class FelixFrameworkManagerForm {

    private JPanel root;

    private TextFieldWithBrowseButton frameworkField;

    public FelixFrameworkManagerForm() {
        this.frameworkField.addBrowseFolderListener( "Select Felix Framework location", null, null, FileChooserDescriptorFactory.createSingleFolderDescriptor() );
    }

    public JPanel getRoot() {
        return root;
    }

    public File getFrameworkPath() {
        String text = this.frameworkField.getText();
        return isNotBlank( text ) ? new File( text.trim() ) : null;
    }

    public void setFrameworkPath( File path ) {
        if( path != null ) {
            this.frameworkField.setText( path.getAbsolutePath() );
        } else {
            this.frameworkField.setText( "" );
        }
    }
}
