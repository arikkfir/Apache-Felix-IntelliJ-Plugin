package com.infolinks.idea.plugins.felix.facet;

import com.infolinks.idea.plugins.felix.facet.pkg.BundlePackage;
import com.intellij.util.ui.ColumnInfo;
import java.util.Comparator;

/**
 * @author arik
 */
public abstract class Columns {

    public static final ColumnInfo<BundlePackage, String> NAME = new ColumnInfo<BundlePackage, String>( "Name" ) {

        @Override
        public String valueOf( BundlePackage bundleInfo ) {
            return bundleInfo.getName();
        }

        @Override
        public Comparator<BundlePackage> getComparator() {
            return new Comparator<BundlePackage>() {

                @Override
                public int compare( BundlePackage o1, BundlePackage o2 ) {
                    return o1.getName().compareTo( o2.getName() );
                }
            };
        }
    };

    public static final ColumnInfo<BundlePackage, String> VERSION = new ColumnInfo<BundlePackage, String>( "Version" ) {

        @Override
        public String valueOf( BundlePackage bundleInfo ) {
            return bundleInfo.getVersion();
        }

        @Override
        public Comparator<BundlePackage> getComparator() {
            return new Comparator<BundlePackage>() {

                @Override
                public int compare( BundlePackage o1, BundlePackage o2 ) {
                    String v1 = o1.getVersion();
                    String v2 = o2.getVersion();
                    if( v1 == null && v2 != null ) {
                        return -1;
                    } else if( v1 != null && v2 == null ) {
                        return 1;
                    } else if( v1 == null ) {
                        //both are null
                        return 0;
                    } else {
                        return v1.compareTo( v2 );
                    }
                }
            };
        }
    };

    private Columns() {
    }
}
