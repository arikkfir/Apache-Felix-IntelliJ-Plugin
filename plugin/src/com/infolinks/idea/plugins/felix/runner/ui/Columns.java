package com.infolinks.idea.plugins.felix.runner.ui;

import com.infolinks.idea.plugins.felix.runner.bundle.BundleInfo;
import com.infolinks.idea.plugins.felix.runner.FelixRunConfiguration;
import com.infolinks.idea.plugins.felix.runner.deploy.ArtifactDeploymentInfo;
import com.infolinks.idea.plugins.felix.runner.deploy.BundleDeploymentInfo;
import com.infolinks.idea.plugins.felix.runner.deploy.ModuleDeploymentInfo;
import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.ui.BooleanTableCellEditor;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.util.ui.ColumnInfo;
import java.io.File;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import org.jetbrains.annotations.NotNull;

/**
 * @author arik
 */
public abstract class Columns {

    public static final ColumnInfo<BundleInfo, String> NAME = new BundleNameColumnInfo();

    public static final ColumnInfo<BundleDeploymentInfo, File> DEPLOY_DIR = new DeployDirColumnInfo();

    public static final ColumnInfo<BundleDeploymentInfo, Boolean> INCLUDE_VERSION = new IncludeVersionColumnInfo();

    public static final ColumnInfo<BundleInfo, String> VERSION = new BundleVersionColumnInfo();

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

    private Columns() {
    }

    public static class DeployDirColumnInfo extends ColumnInfo<BundleDeploymentInfo, File>
            implements RunConfigurationAware {

        private static final String DEFAULT_DEPLOY_DIR_FILE = "default_default";

        private FelixRunConfiguration runConfiguration;

        public DeployDirColumnInfo() {
            super( "Dir" );
        }

        @Override
        public void setRunConfiguration( FelixRunConfiguration configuration ) {
            this.runConfiguration = configuration;
        }

        @Override
        public File valueOf( BundleDeploymentInfo bundleInfo ) {
            return bundleInfo.getDeployDir();
        }

        @Override
        public Comparator<BundleDeploymentInfo> getComparator() {
            return new Comparator<BundleDeploymentInfo>() {

                @Override
                public int compare( BundleDeploymentInfo o1, BundleDeploymentInfo o2 ) {
                    int result = compareBundleInfos( o1, o2 );
                    if( result != 0 ) {
                        return result;
                    }

                    File d1 = o1.getDeployDir();
                    File d2 = o2.getDeployDir();
                    if( d1 == d2 ) {
                        return 0;
                    } else if( d1 != null && d2 != null ) {
                        return d1.compareTo( d2 );
                    } else if( d1 != null ) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            };
        }

        @Override
        public TableCellRenderer getRenderer( BundleDeploymentInfo bundleDeploymentInfo ) {
            return new DeployDirRenderer();
        }

        @Override
        public TableCellEditor getEditor( BundleDeploymentInfo o ) {
            List<File> files = new LinkedList<File>();
            files.add( new File( DEFAULT_DEPLOY_DIR_FILE ) );
            if( this.runConfiguration != null ) {
                files.add( this.runConfiguration.getBundlesDirectory() );
                //TODO arik (6/28/11): this causes NPE when the run configuration has not yet been saved!!!
                files.addAll( this.runConfiguration.getHotDeployDirectories() );
            }

            return new ComboBoxTableRenderer<File>( files.toArray( new File[ files.size() ] ) ) {

                @Override
                protected String getTextFor( @NotNull File value ) {
                    if( value.getName().equals( DEFAULT_DEPLOY_DIR_FILE ) ) {
                        return "(default)";
                    } else {
                        return value.getName();
                    }
                }
            };
        }

        @Override
        public String getPreferredStringValue() {
            return "Bundles";
        }

        @Override
        public int getAdditionalWidth() {
            return 20;
        }

        @Override
        public boolean isCellEditable( BundleDeploymentInfo bundleDeploymentInfo ) {
            return true;
        }

        @Override
        public void setValue( BundleDeploymentInfo bundleDeploymentInfo, File value ) {
            if( value == null || value.getName().equals( DEFAULT_DEPLOY_DIR_FILE ) ) {
                bundleDeploymentInfo.setDeployDir( null );
            } else {
                bundleDeploymentInfo.setDeployDir( value );
            }
        }
    }

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
            return "8.8.8.SNAPSHOT";
        }

        @Override
        public int getAdditionalWidth() {
            return 20;
        }
    }

    public static class IncludeVersionColumnInfo extends ColumnInfo<BundleDeploymentInfo, Boolean> {

        public IncludeVersionColumnInfo() {
            super( "Version?" );
        }

        @Override
        public Boolean valueOf( BundleDeploymentInfo bundleInfo ) {
            return bundleInfo.getIncludeVersionInFilename();
        }

        @Override
        public Comparator<BundleDeploymentInfo> getComparator() {
            return new Comparator<BundleDeploymentInfo>() {

                @Override
                public int compare( BundleDeploymentInfo o1, BundleDeploymentInfo o2 ) {
                    int result = compareBundleInfos( o1, o2 );
                    if( result != 0 ) {
                        return result;
                    } else {
                        return Boolean.valueOf( o1.getIncludeVersionInFilename() ).compareTo( o2.getIncludeVersionInFilename() );
                    }
                }
            };
        }

        @Override
        public TableCellRenderer getRenderer( BundleDeploymentInfo bundleDeploymentInfo ) {
            return new BooleanTableCellRenderer();
        }

        @Override
        public TableCellEditor getEditor( BundleDeploymentInfo o ) {
            return new BooleanTableCellEditor();
        }

        @Override
        public String getPreferredStringValue() {
            return getName();
        }

        @Override
        public int getAdditionalWidth() {
            return 20;
        }

        @Override
        public boolean isCellEditable( BundleDeploymentInfo bundleDeploymentInfo ) {
            return true;
        }

        @Override
        public void setValue( BundleDeploymentInfo bundleDeploymentInfo, Boolean value ) {
            bundleDeploymentInfo.setIncludeVersionInFilename( value );
        }
    }
}
