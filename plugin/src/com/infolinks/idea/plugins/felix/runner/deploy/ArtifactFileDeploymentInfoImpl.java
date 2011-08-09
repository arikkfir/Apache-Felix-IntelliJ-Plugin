package com.infolinks.idea.plugins.felix.runner.deploy;

import com.infolinks.idea.plugins.felix.runner.bundle.BundleInfo;
import com.infolinks.idea.plugins.felix.runner.bundle.BundleInfoManager;
import com.intellij.openapi.project.Project;
import java.io.File;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

/**
 * @author arik
 */
public class ArtifactFileDeploymentInfoImpl implements ArtifactDeploymentInfo {

    private final Project project;

    private final String groupId;

    private final String artifactId;

    private final String mavenVersion;

    public ArtifactFileDeploymentInfoImpl( Project project, String groupId, String artifactId, String mavenVersion ) {
        this.project = project;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.mavenVersion = mavenVersion;
    }

    @Override
    public String getGroupId() {
        return this.groupId;
    }

    @Override
    public String getArtifactId() {
        return this.artifactId;
    }

    @Override
    public String getMavenVersion() {
        return this.mavenVersion;
    }

    @Override
    public boolean isValid() {
        File file = getFile();
        return file != null && file.isFile();
    }

    @Override
    public File getFile() {
        MavenProjectsManager projectsManager = MavenProjectsManager.getInstance( this.project );
        if( projectsManager == null ) {
            return null;
        }

        File localRepository = projectsManager.getLocalRepository();
        if( localRepository == null ) {
            return null;
        }

        File groupDir = new File( localRepository, this.groupId.replace( '.', '/' ) );
        File artifactDir = new File( groupDir, this.artifactId );
        File versionDir = new File( artifactDir, this.mavenVersion );
        return new File( versionDir, this.artifactId + "-" + this.mavenVersion + ".jar" );
    }

    @Override
    public String getDeployFilename() {
        return this.groupId + "-" + this.artifactId + "-" + getOsgiVersion() + ".jar";
    }

    @Override
    public String getName() {
        File file = getFile();
        if( file == null ) {
            if( this.artifactId == null || this.artifactId.trim().length() == 0 ) {
                return "(no artifact file & no artifact-id info)";
            } else {
                return this.artifactId;
            }
        }

        BundleInfo bundleInfo = BundleInfoManager.getInstance( this.project ).getBundleInfo( file );
        if( bundleInfo == null ) {
            if( this.artifactId == null || this.artifactId.trim().length() == 0 ) {
                return "(no artifact bundle & no artifact id)";
            } else {
                return this.artifactId;
            }
        }

        String bundleInfoName = bundleInfo.getName();
        if( bundleInfoName == null || bundleInfoName.trim().length() == 0 ) {
            return "(no artifact bundle name)";
        } else {
            return bundleInfoName;
        }
    }

    @Override
    public String getSymbolicName() {
        File file = getFile();
        if( file == null ) {
            if( this.artifactId == null || this.artifactId.trim().length() == 0 ) {
                return "(no artifact file & no artifact-id info)";
            } else {
                return this.artifactId;
            }
        }

        BundleInfo bundleInfo = BundleInfoManager.getInstance( this.project ).getBundleInfo( file );
        if( bundleInfo == null ) {
            if( this.artifactId == null || this.artifactId.trim().length() == 0 ) {
                return "(no artifact bundle & no artifact id)";
            } else {
                return this.artifactId;
            }
        }

        String bundleInfoSymName = bundleInfo.getSymbolicName();
        if( bundleInfoSymName == null || bundleInfoSymName.trim().length() == 0 ) {
            return "(no artifact bundle symbolic name)";
        } else {
            return bundleInfoSymName;
        }
    }

    @Override
    public String getOsgiVersion() {
        File file = getFile();
        if( file == null ) {
            return this.mavenVersion;
        }

        BundleInfo bundleInfo = BundleInfoManager.getInstance( this.project ).getBundleInfo( file );
        if( bundleInfo == null ) {
            return this.mavenVersion;
        }

        return bundleInfo.getOsgiVersion();
    }

    @Override
    public boolean equals( Object o ) {
        if( this == o ) {
            return true;
        }
        if( o == null || getClass() != o.getClass() ) {
            return false;
        }

        ArtifactFileDeploymentInfoImpl that = ( ArtifactFileDeploymentInfoImpl ) o;

        if( !this.artifactId.equals( that.artifactId ) ) {
            return false;
        }
        if( !this.groupId.equals( that.groupId ) ) {
            return false;
        }
        if( !this.project.equals( that.project ) ) {
            return false;
        }
        //noinspection RedundantIfStatement
        if( !this.mavenVersion.equals( that.mavenVersion ) ) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = this.project.hashCode();
        result = 31 * result + this.groupId.hashCode();
        result = 31 * result + this.artifactId.hashCode();
        result = 31 * result + this.mavenVersion.hashCode();
        return result;
    }
}
