package com.infolinks.idea.plugins.felix.runner.ui;

import com.infolinks.idea.plugins.felix.bundle.BundleInfo;
import com.infolinks.idea.plugins.felix.runner.deploy.ArtifactDeploymentInfo;
import com.infolinks.idea.plugins.felix.runner.deploy.BundleDeploymentInfo;
import com.infolinks.idea.plugins.felix.runner.deploy.ModuleDeploymentInfo;
import com.infolinks.idea.plugins.felix.util.ui.Icons;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ListTableModel;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * @author arik
 */
public class BundleNameRenderer extends DefaultTableCellRenderer {

    private final BundleInfo bundleInfo;

    public BundleNameRenderer( BundleInfo bundleInfo ) {
        this.bundleInfo = bundleInfo;
    }

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
            if( bundleInfo instanceof ModuleDeploymentInfo ) {
                label.setIcon( Icons.MODULE_ICON );
            } else if( bundleInfo instanceof ArtifactDeploymentInfo ) {
                label.setIcon( Icons.BUNDLE_ICON );
            } else {
                label.setIcon( Icons.QUESTION_MARK );
            }
            label.setText( value == null ? "" : value.toString() );

            TableView tableView = ( TableView ) table;
            ListTableModel model = tableView.getListTableModel();
            BundleDeploymentInfo item = ( BundleDeploymentInfo ) model.getItem( table.convertRowIndexToModel( row ) );
            if( !item.isValid() ) {
                label.setForeground( Color.RED );
            }
        }
        return component;
    }
}
