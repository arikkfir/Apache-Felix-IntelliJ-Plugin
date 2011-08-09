/*
 * Copyright (c) 2011 Arik Kfir. All rights reserved.
 */

package com.infolinks.idea.plugins.felix.runner.ui;

import com.infolinks.idea.plugins.felix.runner.bundle.BundleInfo;
import com.infolinks.idea.plugins.felix.runner.deploy.ArtifactDeploymentInfo;
import com.infolinks.idea.plugins.felix.runner.deploy.BundleDeploymentInfo;
import com.infolinks.idea.plugins.felix.runner.deploy.ModuleDeploymentInfo;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import java.util.Comparator;
import javax.swing.table.TableCellRenderer;

/**
 * @author arik
 */
public class BundlesTable extends TableView<BundleDeploymentInfo> {

    public static class BundleNameColumnInfo extends ColumnInfo<BundleInfo, String> {

        public BundleNameColumnInfo() {
            super( "Name" );
        }

        @Override
        public String valueOf( BundleInfo bundleInfo ) {
            return getBundleDisplayName( bundleInfo );
        }

        @Override
        public Comparator<BundleInfo> getComparator() {
            return new Comparator<BundleInfo>() {

                @Override
                public int compare( BundleInfo o1, BundleInfo o2 ) {
                    int result = compareBundleInfos( o1, o2 );
                    if( result != 0 ) {
                        return result;
                    } else {
                        String o1BundleName = getBundleDisplayName( o1 );
                        String o2BundleName = getBundleDisplayName( o2 );
                        if( o1BundleName == null && o2BundleName == null ) {
                            return 0;
                        } else if( o1BundleName != null && o2BundleName == null ) {
                            return 1;
                        } else if( o1BundleName == null ) {
                            return -1;
                        } else {
                            return o1BundleName.compareToIgnoreCase( o2BundleName );
                        }
                    }
                }
            };
        }

        @Override
        public String getPreferredStringValue() {
            // this is the test string used to determine initial width of the column
            return "Apache Felix IntelliJ Plugin";
        }

        @Override
        public TableCellRenderer getRenderer( final BundleInfo bundleInfo ) {
            return new BundleNameRenderer( bundleInfo );
        }
    }

    public static class BundleVersionColumnInfo extends ColumnInfo<BundleInfo, String> {

        public BundleVersionColumnInfo() {
            super( "Version" );
        }

        @Override
        public String valueOf( BundleInfo bundleInfo ) {
            return bundleInfo.getOsgiVersion();
        }

        @Override
        public Comparator<BundleInfo> getComparator() {
            return new Comparator<BundleInfo>() {

                @Override
                public int compare( BundleInfo o1, BundleInfo o2 ) {
                    int result = compareBundleInfos( o1, o2 );
                    if( result != 0 ) {
                        return result;
                    } else {
                        return o1.getOsgiVersion().compareToIgnoreCase( o2.getOsgiVersion() );
                    }
                }
            };
        }

        @Override
        public String getPreferredStringValue() {
            // this is the test string used to determine initial width of the column - the '8' char is the widest...
            return "8.8.8.SNAPSHOT";
        }

        @Override
        public int getAdditionalWidth() {
            return 20;
        }
    }

    private static String getBundleDisplayName( BundleInfo bundleInfo ) {
        String name = bundleInfo.getName();
        if( name == null || name.trim().length() == 0 ) {
            return bundleInfo.getSymbolicName();
        } else {
            return name;
        }
    }

    private static int compareBundleInfos( BundleInfo b1, BundleInfo b2 ) {
        if( b1 instanceof ModuleDeploymentInfo && b2 instanceof ArtifactDeploymentInfo ) {
            return -1;
        } else if( b1 instanceof ArtifactDeploymentInfo && b2 instanceof ModuleDeploymentInfo ) {
            return 1;
        } else {
            return 0;
        }
    }

    public BundlesTable() {
        super( new BundleTableModel() );
    }

    private static class BundleTableModel extends ListTableModel<BundleDeploymentInfo> {

        private BundleTableModel() {
            super( new BundleNameColumnInfo(), new BundleVersionColumnInfo() );
        }
    }

}
