package com.infolinks.idea.plugins.felix.runner;

import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.infolinks.idea.plugins.felix.framework.FelixFrameworkManager;
import com.infolinks.idea.plugins.felix.runner.deploy.ArtifactDeploymentInfo;
import com.infolinks.idea.plugins.felix.runner.deploy.BundleDeploymentInfo;
import com.infolinks.idea.plugins.felix.runner.deploy.ModuleDeploymentInfo;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.jetbrains.annotations.Nullable;

import static java.util.Locale.ENGLISH;
import static org.codehaus.plexus.util.FileUtils.*;

/**
 * @author arik
 */
public class FelixBundlesDeployer extends AbstractProjectComponent {

    public static interface ModuleDeploymentFilter {

        boolean shouldDeploy( BundleDeploymentInfo deploymentInfo );
    }

    public static FelixBundlesDeployer getInstance( Project project ) {
        return project.getComponent( FelixBundlesDeployer.class );
    }

    private final Logger logger = Logger.getInstance( "#" + getClass().getName() );

    private static final String FILE_INSTALL_FILENAME = "org.apache.felix.fileinstall-3.1.10.jar";

    public FelixBundlesDeployer( Project project ) {
        super( project );
    }

    public void setupDeployment( FelixRunConfiguration runConfiguration ) throws DeploymentException {
        //
        // deploy bundles selected in the run configuration
        //
        Map<File, Set<File>> deployedBundles = deploySelectedBundles( runConfiguration );

        //
        // copy felix distribution jars to 'bundles' directory
        //
        deployedBundles.get( runConfiguration.getBundlesDirectory() ).addAll( deployFelixDistJars( runConfiguration ) );

        //
        // copy file-install bundle to 'bundles' directory
        //
        File fileInstallBundle = copyOrDeleteFileInstallBundle( runConfiguration );
        if( fileInstallBundle != null ) {
            deployedBundles.get( runConfiguration.getBundlesDirectory() ).add( fileInstallBundle );
        }

        //
        // remove any file not part of the deployment (from older runs usually)
        //
        removeUnrelatedBundles( deployedBundles );
    }

    private List<File> deployFelixDistJars( FelixRunConfiguration runConfiguration ) throws DeploymentException {
        File felixBinDir = new File( FelixFrameworkManager.getInstance().getFrameworkPath(), "bundle" );

        File[] files = felixBinDir.listFiles( new FilenameFilter() {

            @Override
            public boolean accept( File dir, String name ) {
                return !name.equalsIgnoreCase( "felix.jar" ) && name.endsWith( ".jar" );
            }
        } );

        List<File> deployedFiles = new LinkedList<File>();
        for( File file : files ) {
            File to = new File( runConfiguration.getBundlesDirectory(), file.getName() );
            deployedFiles.add( to );
            try {
                Files.copy( file, to );
            } catch( IOException e ) {
                throw new DeploymentException( "Could not copy Felix file '" + file + "' to '" + toString() + "'" );
            }
        }
        return deployedFiles;
    }

    public Map<File, Set<File>> deploySelectedBundles( FelixRunConfiguration runConfiguration )
            throws DeploymentException {
        return deploySelectedBundles( runConfiguration, null );
    }

    public Map<File, Set<File>> deploySelectedBundles( FelixRunConfiguration runConfiguration,
                                                       @Nullable ModuleDeploymentFilter filter )
            throws DeploymentException {

        ensureDirectoryExists( runConfiguration.getBundlesDirectory() );
        List<File> hotDeployDirectories = runConfiguration.getHotDeployDirectories();
        if( runConfiguration.isEnableHotDeploy() ) {
            for( File hotDeployDirectory : hotDeployDirectories ) {
                ensureDirectoryExists( hotDeployDirectory );
            }
        }

        Map<File, Set<File>> deployedFiles = new HashMap<File, Set<File>>();
        for( BundleDeploymentInfo bundle : runConfiguration.getBundles() ) {
            File file = bundle.getFile();
            if( file != null && file.isFile() && ( filter == null || filter.shouldDeploy( bundle ) ) ) {
                File deployDir = bundle.getDeployDir();
                if( deployDir == null ) {
                    if( bundle instanceof ArtifactDeploymentInfo ) {
                        deployDir = runConfiguration.getBundlesDirectory();
                    } else if( bundle instanceof ModuleDeploymentInfo ) {
                        if( runConfiguration.isEnableHotDeploy() && !hotDeployDirectories.isEmpty() ) {
                            deployDir = hotDeployDirectories.get( 0 );
                        } else {
                            deployDir = runConfiguration.getBundlesDirectory();
                        }
                    } else {
                        throw new DeploymentException( "Internal error - unknown bundle deployment info (" + bundle.getClass().getName() + ")" );
                    }
                }

                Set<File> filesDeployedToThisDeployDir = deployedFiles.get( deployDir );
                if( filesDeployedToThisDeployDir == null ) {
                    filesDeployedToThisDeployDir = new HashSet<File>();
                    deployedFiles.put( deployDir, filesDeployedToThisDeployDir );
                }

                File targetFile = new File( deployDir, bundle.getDeployFilename() );
                this.logger.info( "Deploying '" + file + "' to '" + targetFile + "'" );

                if( filesDeployedToThisDeployDir.contains( targetFile ) ) {
                    throw new DeploymentException( "More than one bundle with name '" + targetFile.getName() + "' is set to be deployed to '" + deployDir + "'" );
                }

                try {
                    copyFileIfModified( file, targetFile );
                } catch( IOException e ) {
                    throw new DeploymentException( "Could not deploy bundle '" + file + "': " + e.getMessage(), e );
                }
                filesDeployedToThisDeployDir.add( targetFile );
            }
        }

        return deployedFiles;
    }

    private File copyOrDeleteFileInstallBundle( FelixRunConfiguration runConfiguration ) throws DeploymentException {
        File destFileInstallFile = new File( runConfiguration.getBundlesDirectory(), FILE_INSTALL_FILENAME );
        if( runConfiguration.isEnableHotDeploy() ) {
            try {
                this.logger.info( "Copying '" + getFileInstallBundleResource() + "' to '" + destFileInstallFile + "'" );
                Files.copy( Resources.newInputStreamSupplier( getFileInstallBundleResource() ), destFileInstallFile );
                return destFileInstallFile;
            } catch( IOException e ) {
                throw new DeploymentException( "Could not copy Apache Felix File Install bundle to bundles directory: " + e.getMessage(), e );
            }

        } else {
            if( destFileInstallFile.exists() ) {
                try {
                    this.logger.info( "Deleting '" + destFileInstallFile + "'" );
                    Files.deleteRecursively( destFileInstallFile );
                } catch( IOException e ) {
                    throw new DeploymentException( "Could not remove Apache Felix File Install bundle from bundles directory: " + e.getMessage(), e );
                }
            }
            return null;
        }
    }

    private void removeUnrelatedBundles( Map<File, Set<File>> deployedBundlesByDeployDir ) throws DeploymentException {

        FilenameFilter jarFilesFilter = new FilenameFilter() {

            @Override
            public boolean accept( File dir, String name ) {
                return name.toLowerCase( ENGLISH ).endsWith( ".jar" );
            }
        };

        for( File deployDir : deployedBundlesByDeployDir.keySet() ) {
            Set<File> deployedBundles = deployedBundlesByDeployDir.get( deployDir );
            for( File jar : deployDir.listFiles( jarFilesFilter ) ) {
                if( !deployedBundles.contains( jar ) ) {
                    try {
                        this.logger.info( "Deleting '" + jar + "'" );
                        forceDelete( jar );
                    } catch( IOException e ) {
                        throw new DeploymentException( "Could not delete old bundle '" + jar.getName() + "': " + e.getMessage(), e );
                    }
                }
            }
        }
    }

    private URL getFileInstallBundleResource() throws DeploymentException {
        URL resource = getClass().getClassLoader().getResource( "bundles/" + FILE_INSTALL_FILENAME );
        if( resource == null ) {
            throw new DeploymentException( "Apache Felix FileInstall bundle could not be found in Apache Felix bundle (this should not happen, please report this)." );
        } else {
            return resource;
        }
    }

    private void ensureDirectoryExists( File dir ) throws DeploymentException {
        if( dir.isFile() ) {
            try {
                this.logger.info( "Deleting '" + dir + "'" );
                Files.deleteRecursively( dir );
            } catch( IOException e ) {
                throw new DeploymentException( "Path '" + dir.getAbsolutePath() + "' must be a directory, but there's a file by that name instead" );
            }
        }

        if( !dir.exists() ) {
            try {
                this.logger.info( "Creating directory '" + dir + "'" );
                forceMkdir( dir );
            } catch( IOException e ) {
                throw new DeploymentException( "Could not create deployment directories: " + e.getMessage(), e );
            }
        }
    }
}
