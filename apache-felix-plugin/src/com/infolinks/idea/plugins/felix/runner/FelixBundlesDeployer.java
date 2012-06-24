package com.infolinks.idea.plugins.felix.runner;

import com.google.common.io.Files;
import com.infolinks.idea.plugins.felix.framework.FelixFrameworkManager;
import com.infolinks.idea.plugins.felix.runner.deploy.BundleDeploymentInfo;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import java.io.*;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.codehaus.plexus.util.IOUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Locale.ENGLISH;
import static org.codehaus.plexus.util.FileUtils.*;

/**
 * @author arik
 */
public class FelixBundlesDeployer extends AbstractProjectComponent
{

    public static interface ModuleDeploymentFilter
    {

        boolean shouldDeploy( BundleDeploymentInfo deploymentInfo );
    }

    public static FelixBundlesDeployer getInstance( Project project )
    {
        return project.getComponent( FelixBundlesDeployer.class );
    }

    private final Logger logger = Logger.getInstance( "#" + getClass().getName() );

    public FelixBundlesDeployer( Project project )
    {
        super( project );
    }

    public void setupDeployment( FelixRunConfiguration runConfiguration ) throws DeploymentException
    {
        //
        // deploy bundles selected in the run configuration
        //
        Set<File> deployedBundles = deploySelectedBundles( runConfiguration );

        //
        // copy felix distribution jars to 'bundles' directory
        //
        deployedBundles.addAll( deployFelixDistJars( runConfiguration ) );

        //
        // copy bundle server JAR to 'bundles' directory
        //
        File bundleServerFile = new File( runConfiguration.getBundlesDirectory(), "bundle-server.jar" );
        try
        {
            copyURLToFile( getBundleServerResource(), bundleServerFile );
            deployedBundles.add( bundleServerFile );
        }
        catch( Exception e )
        {
            throw new DeploymentException( "Could not deploy bundle-server OSGi bundle to Felix: " + e.getMessage(), e );
        }

        //
        // remove any file not part of the deployment (from older runs usually)
        //
        removeUnrelatedBundles( runConfiguration, deployedBundles );
    }

    private List<File> deployFelixDistJars( FelixRunConfiguration runConfiguration ) throws DeploymentException
    {
        File felixBinDir = new File( FelixFrameworkManager.getInstance().getFrameworkPath(), "bundle" );

        File[] files = felixBinDir.listFiles( new FilenameFilter()
        {

            @Override
            public boolean accept( File dir, String name )
            {
                return !name.equalsIgnoreCase( "felix.jar" ) && name.endsWith( ".jar" );
            }
        } );
        if( files == null )
        {
            throw new DeploymentException( "Felix 'bin' directory is empty or does not exist!" );
        }

        List<File> deployedFiles = new LinkedList<File>();
        for( File file : files )
        {
            File to = new File( runConfiguration.getBundlesDirectory(), file.getName() );
            deployedFiles.add( to );
            try
            {
                Files.copy( file, to );
            }
            catch( IOException e )
            {
                throw new DeploymentException( "Could not copy Felix file '" + file + "' to '" + toString() + "'" );
            }
        }
        return deployedFiles;
    }

    public Set<File> deploySelectedBundles( FelixRunConfiguration runConfiguration ) throws DeploymentException
    {
        return deploySelectedBundles( runConfiguration, null );
    }

    public Set<File> deploySelectedBundles( FelixRunConfiguration runConfiguration,
                                            @Nullable ModuleDeploymentFilter filter )
    throws DeploymentException
    {

        ensureDirectoryExists( runConfiguration.getBundlesDirectory() );

        Set<File> deployedFiles = new HashSet<File>();
        Set<File> addedFiles = new HashSet<File>();
        Set<BundleDeploymentInfo> modifiedFiles = new HashSet<BundleDeploymentInfo>();
        for( BundleDeploymentInfo bundle : runConfiguration.getBundles() )
        {
            File file = bundle.getFile();
            if( file != null && file.isFile() && ( filter == null || filter.shouldDeploy( bundle ) ) )
            {
                File deployDir = runConfiguration.getBundlesDirectory();

                File targetFile = new File( deployDir, bundle.getDeployFilename() );
                this.logger.info( "Deploying '" + file + "' to '" + targetFile + "'" );

                if( deployedFiles.contains( targetFile ) )
                {
                    throw new DeploymentException( "More than one bundle with name '" + targetFile.getName() + "' is set to be deployed to '" + deployDir + "'" );
                }

                try
                {
                    boolean newBundle = !targetFile.exists();
                    if( copyFileIfModified( file, targetFile ) )
                    {
                        if( newBundle )
                        {
                            addedFiles.add( file );
                        }
                        else
                        {
                            modifiedFiles.add( bundle );
                        }
                    }
                    deployedFiles.add( targetFile );
                }
                catch( IOException e )
                {
                    throw new DeploymentException( "Could not deploy bundle '" + file + "': " + e.getMessage(), e );
                }
            }
        }

        if( !addedFiles.isEmpty() || !modifiedFiles.isEmpty() )
        {
            Socket socket = null;
            BufferedReader reader = null;
            Writer writer = null;
            try
            {
                socket = new Socket( InetAddress.getLocalHost(), runConfiguration.getBundleServerPort() );
                socket.setKeepAlive( false );
                socket.setSoTimeout( 2000 );//TODO arik (8/9/11): make this configurable
                reader = new BufferedReader( new InputStreamReader( socket.getInputStream(), "UTF-8" ), 8096 );
                writer = new OutputStreamWriter( socket.getOutputStream(), "UTF-8" );

                for( File file : addedFiles )
                {
                    writer.write( "INSTALL:" + file.getAbsolutePath() + "\n" );
                }
                for( BundleDeploymentInfo bundle : modifiedFiles )
                {
                    writer.write( "UPDATE:" + bundle.getSymbolicName() + ":" + bundle.getOsgiVersion() + "\n" );
                }
                writer.flush();
            }
            catch( ConnectException ignore )
            {
                //ignore

            }
            catch( Exception e )
            {
                //TODO arik (8/9/11): report error
                e.printStackTrace();
                throw new DeploymentException( "Deployment error while updating bundles in running Felix instance: " + e.getMessage(), e );
            }
            finally
            {
                try
                {
                    if( socket != null )
                    {
                        socket.close();
                    }
                }
                catch( Exception ignore )
                {
                    //no-op
                }
                IOUtil.close( reader );
                IOUtil.close( writer );
            }
        }
        return deployedFiles;
    }

    private void removeUnrelatedBundles( FelixRunConfiguration runConfiguration, Set<File> deployedBundlesByDeployDir )
    throws DeploymentException
    {

        FilenameFilter jarFilesFilter = new FilenameFilter()
        {

            @Override
            public boolean accept( File dir, String name )
            {
                return name.toLowerCase( ENGLISH ).endsWith( ".jar" );
            }
        };

        for( File jar : runConfiguration.getBundlesDirectory().listFiles( jarFilesFilter ) )
        {
            if( !deployedBundlesByDeployDir.contains( jar ) )
            {
                try
                {
                    this.logger.info( "Deleting '" + jar + "'" );
                    forceDelete( jar );
                }
                catch( IOException e )
                {
                    throw new DeploymentException( "Could not delete old bundle '" + jar.getName() + "': " + e.getMessage(), e );
                }
            }
        }
    }

    @NotNull
    private URL getBundleServerResource() throws DeploymentException
    {
        URL resource = getClass().getClassLoader().getResource( "bundles/bundle-server.jar" );
        if( resource == null )
        {
            throw new DeploymentException( "Could not find Bundle Server OSGi bundle (should be bundled inside the Apache Felix IntelliJ Plugin, but it is missing). This should not happen, please report this." );
        }
        else
        {
            return resource;
        }
    }

    private void ensureDirectoryExists( File dir ) throws DeploymentException
    {
        if( dir.isFile() )
        {
            this.logger.info( "Deleting '" + dir + "'" );
            if( !dir.delete() )
            {
                throw new DeploymentException( "Path '" + dir.getAbsolutePath() + "' must be a directory, but there's a file by that name instead and it can't be deleted" );
            }
        }

        if( !dir.exists() )
        {
            try
            {
                this.logger.info( "Creating directory '" + dir + "'" );
                forceMkdir( dir );
            }
            catch( IOException e )
            {
                throw new DeploymentException( "Could not create deployment directories: " + e.getMessage(), e );
            }
        }
    }
}
