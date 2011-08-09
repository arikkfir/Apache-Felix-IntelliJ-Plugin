package com.infolinks.idea.plugins.felix.runner.bundle;


import aQute.lib.osgi.Jar;
import com.infolinks.idea.plugins.felix.build.BundleInstructionsHelper;
import com.infolinks.idea.plugins.felix.facet.OsgiBundleFacet;
import com.infolinks.idea.plugins.felix.runner.deploy.ArtifactFileDeploymentInfoImpl;
import com.infolinks.idea.plugins.felix.runner.deploy.BundleDeploymentInfo;
import com.infolinks.idea.plugins.felix.runner.deploy.BundleDeploymentInfoComparator;
import com.infolinks.idea.plugins.felix.runner.deploy.ModuleDeploymentInfoImpl;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import java.io.File;
import java.util.*;
import org.apache.maven.artifact.Artifact;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import static com.infolinks.idea.plugins.felix.facet.OsgiBundleFacet.getOsgiBundleFacet;

/**
 * @author arik
 */
public class BundleInfoManager extends AbstractProjectComponent {

    private final Object LOCK = new Object();

    private final Map<File, BundleInfo> bundleInfoMap = new HashMap<File, BundleInfo>();

    public static BundleInfoManager getInstance( Project project ) {
        return project.getComponent( BundleInfoManager.class );
    }

    public BundleInfoManager( Project project ) {
        super( project );
    }

    @Override
    public void projectOpened() {
        MessageBusConnection busConnection = this.myProject.getMessageBus().connect();
        busConnection.subscribe( VirtualFileManager.VFS_CHANGES, new BundleInfoFileListener() );
    }

    public BundleInfo getBundleInfo( @NotNull File file ) {
        synchronized( LOCK ) {
            BundleInfo bundleInfo = this.bundleInfoMap.get( file );
            if( bundleInfo == null ) {
                bundleInfo = readBundleInfo( file );
                this.bundleInfoMap.put( file, bundleInfo );
            }
            return bundleInfo;
        }
    }

    @SuppressWarnings( { "WeakerAccess" } )
    public boolean hasBundleInfo( @NotNull File file ) {
        synchronized( LOCK ) {
            return this.bundleInfoMap.containsKey( file );
        }
    }

    public List<BundleDeploymentInfo> getAvailableBundles() {
        Set<BundleDeploymentInfo> bundleDeploymentInfos = new HashSet<BundleDeploymentInfo>();

        for( Module module : ModuleManager.getInstance( this.myProject ).getSortedModules() ) {
            BundleInstructionsHelper helper = BundleInstructionsHelper.getInstance( module );
            if( helper != null ) {
                OsgiBundleFacet osgiBundleFacet = getOsgiBundleFacet( module );
                MavenProject mavenProject = helper.getMavenProject();
                if( osgiBundleFacet != null ) {
                    bundleDeploymentInfos.add( new ModuleDeploymentInfoImpl( this.myProject, module.getName() ) );

                    for( MavenArtifact dependency : mavenProject.getDependencies() ) {
                        if( !dependency.getScope().equalsIgnoreCase( Artifact.SCOPE_TEST ) ) {
                            MavenProject project = MavenProjectsManager.getInstance( this.myProject ).findProject( dependency );
                            if( project == null ) {
                                bundleDeploymentInfos.add( new ArtifactFileDeploymentInfoImpl( this.myProject, dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion() ) );
                            }
                        }
                    }
                }
            }
        }

        List<BundleDeploymentInfo> bundlesList = new ArrayList<BundleDeploymentInfo>( bundleDeploymentInfos );
        Collections.sort( bundlesList, new BundleDeploymentInfoComparator() );
        return bundlesList;
    }

    private BundleInfo readBundleInfo( File file ) {
        try {
            Jar jar = new Jar( file );
            String symbolicName = jar.getManifest().getMainAttributes().getValue( "Bundle-SymbolicName" );
            if( symbolicName == null || symbolicName.trim().length() == 0 ) {
                Logger.getInstance( getClass().getName() ).debug( "Jar '" + file + "' is not an OSGi bundle" );
                return null;
            }

            BundleInfoImpl bundleInfo = new BundleInfoImpl();
            bundleInfo.setName( jar.getManifest().getMainAttributes().getValue( "Bundle-Name" ) );
            bundleInfo.setSymbolicName( symbolicName );
            bundleInfo.setOsgiVersion( jar.getManifest().getMainAttributes().getValue( "Bundle-Version" ) );
            bundleInfo.setFilename( jar.getName() );
            return bundleInfo;

        } catch( Exception e ) {
            Logger.getInstance( getClass().getName() ).warn( "Error reading bundle information from: " + file, e );
        }

        return null;
    }

    private class BundleInfoFileListener implements BulkFileListener {

        @Override
        public void before( List<? extends VFileEvent> events ) {
            //no-op
        }

        @Override
        public void after( List<? extends VFileEvent> events ) {
            Map<File, BundleInfo> newBundleInfos = new HashMap<File, BundleInfo>();
            for( VFileEvent event : events ) {
                VirtualFile file = event.getFile();
                if( file != null ) {
                    File ioFile = VfsUtil.virtualToIoFile( file );
                    if( hasBundleInfo( ioFile ) ) {
                        newBundleInfos.put( ioFile, readBundleInfo( ioFile ) );
                    }
                }
            }
            synchronized( LOCK ) {
                bundleInfoMap.putAll( newBundleInfos );
            }
        }
    }
}
