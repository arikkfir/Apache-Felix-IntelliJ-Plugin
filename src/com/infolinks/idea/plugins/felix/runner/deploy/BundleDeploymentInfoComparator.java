package com.infolinks.idea.plugins.felix.runner.deploy;

import java.util.Comparator;
import org.apache.commons.lang.StringUtils;

/**
 * @author arik
 */
public class BundleDeploymentInfoComparator implements Comparator<BundleDeploymentInfo> {

    @SuppressWarnings( { "ConstantConditions" } )
    @Override
    public int compare( BundleDeploymentInfo o1, BundleDeploymentInfo o2 ) {

        boolean v1 = o1.isValid(), v2 = o2.isValid();
        if( v1 && !v2 ) {
            return 1;
        } else if( !v1 && v2 ) {
            return -1;
        }

        boolean o1_Is_Module = o1 instanceof ModuleDeploymentInfo;
        boolean o1_Is_Artifact = o1 instanceof ArtifactDeploymentInfo;
        boolean o2_Is_Module = o2 instanceof ModuleDeploymentInfo;
        boolean o2_Is_Artifact = o2 instanceof ArtifactDeploymentInfo;

        if( o1_Is_Module && o2_Is_Artifact ) {
            return -1;
        } else if( o1_Is_Artifact && o2_Is_Module ) {
            return 1;
        }

        String n1 = o1.getName() == null ? "" : o1.getName();
        String n2 = o2.getName() == null ? "" : o2.getName();
        int nameResult = n1.compareToIgnoreCase( n2 );
        if( nameResult != 0 ) {
            return nameResult;
        }

        return new Version( o1.getOsgiVersion() ).compareTo( new Version( o2.getOsgiVersion() ) );
    }

    private class Version implements Comparable<Version> {

        private final int major;

        private final int minor;

        private final int patch;

        private final String classifier;

        private Version( String ver ) {
            if( ver == null || ver.trim().length() == 0 ) {
                this.major = 0;
                this.minor = 0;
                this.patch = 0;
                this.classifier = null;
                return;
            }

            String[] tokens = StringUtils.split( ver, '.' );
            switch( tokens.length ) {
                case 0:
                    this.major = 0;
                    this.minor = 0;
                    this.patch = 0;
                    this.classifier = null;
                    return;

                case 1:
                    this.major = parseIntToken( tokens[ 0 ] );
                    this.minor = 0;
                    this.patch = 0;
                    this.classifier = null;
                    break;
                case 2:
                    this.major = parseIntToken( tokens[ 0 ] );
                    this.minor = parseIntToken( tokens[ 1 ] );
                    this.patch = 0;
                    this.classifier = null;
                    break;
                case 3:
                    this.major = parseIntToken( tokens[ 0 ] );
                    this.minor = parseIntToken( tokens[ 1 ] );
                    this.patch = parseIntToken( tokens[ 2 ] );
                    this.classifier = null;
                    break;
                default:
                    this.major = parseIntToken( tokens[ 0 ] );
                    this.minor = parseIntToken( tokens[ 1 ] );
                    this.patch = parseIntToken( tokens[ 2 ] );
                    this.classifier = tokens[ 3 ];
                    break;
            }
        }

        @Override
        public int compareTo( Version o ) {
            if( this.major < o.major ) {
                return -1;
            } else if( this.major > o.major ) {
                return 1;
            }

            if( this.minor < o.minor ) {
                return -1;
            } else if( this.minor > o.minor ) {
                return 1;
            }

            if( this.patch < o.patch ) {
                return -1;
            } else if( this.patch > o.patch ) {
                return 1;
            }

            if( this.classifier == null && o.classifier != null ) {
                return -1;

            } else if( this.classifier != null && o.classifier == null ) {
                return 1;

            } else if( this.classifier != null && o.classifier != null ) {
                return this.classifier.compareTo( o.classifier );

            } else {
                return 0;
            }
        }

        private int parseIntToken( String token ) {
            try {
                return Integer.parseInt( token );
            } catch( NumberFormatException e ) {
                return 0;
            }
        }
    }
}
