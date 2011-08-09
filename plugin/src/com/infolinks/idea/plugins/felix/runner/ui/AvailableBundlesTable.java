package com.infolinks.idea.plugins.felix.runner.ui;

import com.infolinks.idea.plugins.felix.runner.deploy.BundleDeploymentInfo;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ListTableModel;

/**
 * @author arik
 */
public class AvailableBundlesTable extends TableView<BundleDeploymentInfo> {

    public AvailableBundlesTable() {
        super( new BundleTableModel() );
    }

    private static class BundleTableModel extends ListTableModel<BundleDeploymentInfo> {

        private BundleTableModel() {
            super( Columns.NAME, Columns.VERSION );
        }
    }

}