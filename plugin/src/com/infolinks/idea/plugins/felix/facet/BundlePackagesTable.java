package com.infolinks.idea.plugins.felix.facet;

import com.infolinks.idea.plugins.felix.facet.pkg.BundlePackage;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ListTableModel;

/**
 * @author arik
 */
public class BundlePackagesTable extends TableView<BundlePackage> {

    public BundlePackagesTable() {
        super( new PackagesTableModel() );
    }

    private static class PackagesTableModel extends ListTableModel<BundlePackage> {

        private PackagesTableModel() {
            super( Columns.NAME, Columns.VERSION );
        }
    }

}
