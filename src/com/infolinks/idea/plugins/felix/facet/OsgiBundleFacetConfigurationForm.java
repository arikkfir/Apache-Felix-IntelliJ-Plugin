package com.infolinks.idea.plugins.felix.facet;

import javax.swing.*;

/**
 * @author arik
 */
public class OsgiBundleFacetConfigurationForm {

    private JPanel root;

    private JTextField bundleNameField;

    private JTextField bundleSymbolicNameField;

    private JTextField bundleVersionField;

    private BundlePackagesTable exportedPackagesTable;

    private BundlePackagesTable importedPackagesTable;

    public JPanel getRoot() {
        return root;
    }

    public void resetFrom( OsgiBundleFacet facet ) {
        this.bundleNameField.setText( facet.getBundleName() );
        this.bundleSymbolicNameField.setText( facet.getBundleSymbolicName() );
        this.bundleVersionField.setText( facet.getBundleVersion() );
        this.exportedPackagesTable.getListTableModel().setItems( facet.getExportedPackages() );
        this.importedPackagesTable.getListTableModel().setItems( facet.getImportedPackages() );
    }
}
