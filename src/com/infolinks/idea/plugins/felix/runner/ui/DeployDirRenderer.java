package com.infolinks.idea.plugins.felix.runner.ui;

import java.awt.*;
import java.io.File;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * @author arik
 */
public class DeployDirRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent( JTable table,
                                                    Object value,
                                                    boolean isSelected,
                                                    boolean hasFocus,
                                                    int row,
                                                    int column ) {
        Component component = super.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
        if( component instanceof JLabel ) {
            JLabel label = ( JLabel ) component;
            if( value != null ) {
                File deployDir = ( File ) value;
                label.setText( deployDir.getName() );
                label.setToolTipText( deployDir.getAbsolutePath() );
            } else {
                label.setText( "(default)" );
                label.setToolTipText( "" );
            }
        }
        return component;
    }
}
