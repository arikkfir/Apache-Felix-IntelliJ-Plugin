package com.infolinks.idea.plugins.felix.runner.deploy;

import com.infolinks.idea.plugins.felix.facet.OsgiBundleFacet;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import java.io.File;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

/**
 * @author arik
 */
public class ModuleDeploymentInfoImpl implements ModuleDeploymentInfo {

    private final Project project;

    private final String moduleName;

    private File deployDir;

    private boolean includeVersionInFilename;

    public ModuleDeploymentInfoImpl( Project project,
                                     String moduleName,
                                     File deployDir,
                                     boolean includeVersionInFilename ) {
        this.project = project;
        this.moduleName = moduleName;
        this.deployDir = deployDir;
        this.includeVersionInFilename = includeVersionInFilename;
    }

    @Override
    public boolean isValid() {
        return getModule() != null;
    }

    @Override
    public String getModuleName() {
        return this.moduleName;
    }

    @Override
    public Module getModule() {
        return ModuleManager.getInstance( this.project ).findModuleByName( this.moduleName );
    }

    @Override
    public File getFile() {
        Module module = getModule();
        if( module == null ) {
            return null;
        } else {
            MavenProject mavenProject = MavenProjectsManager.getInstance( this.project ).findProject( module );
            if( mavenProject != null ) {
                File buildDirectory = new File( mavenProject.getBuildDirectory() );
                return new File( buildDirectory, mavenProject.getFinalName() + ".jar" );
            } else {
                return null;
            }
        }
    }

    @Override
    public String getDeployFilename() {
        Module module = getModule();
        if( module == null ) {
            return null;
        }

        MavenProject mavenProject = MavenProjectsManager.getInstance( this.project ).findProject( module );
        if( mavenProject == null ) {
            return null;
        }

        MavenId mavenId = mavenProject.getMavenId();
        if( this.includeVersionInFilename ) {
            return mavenId.getGroupId() + "-" + mavenId.getArtifactId() + "-" + mavenId.getVersion() + ".jar";
        } else {
            return mavenId.getGroupId() + "-" + mavenId.getArtifactId() + ".jar";
        }
    }

    @Override
    public File getDeployDir() {
        return this.deployDir;
    }

    @Override
    public void setDeployDir( File deployDir ) {
        this.deployDir = deployDir;
    }

    @Override
    public boolean getIncludeVersionInFilename() {
        return this.includeVersionInFilename;
    }

    public void setIncludeVersionInFilename( boolean includeVersionInFilename ) {
        this.includeVersionInFilename = includeVersionInFilename;
    }

    @Override
    public String getName() {
        OsgiBundleFacet facet = getOsgiFacet();
        return facet == null ? "(unknown)" : facet.getBundleName();
    }

    @Override
    public String getSymbolicName() {
        OsgiBundleFacet facet = getOsgiFacet();
        return facet == null ? "(unknown)" : facet.getBundleSymbolicName();
    }

    @Override
    public String getOsgiVersion() {
        OsgiBundleFacet facet = getOsgiFacet();
        return facet == null ? "(unknown)" : facet.getBundleVersion();
    }

    @Override
    public boolean equals( Object o ) {
        if( this == o ) {
            return true;
        }
        if( o == null || getClass() != o.getClass() ) {
            return false;
        }

        ModuleDeploymentInfoImpl that = ( ModuleDeploymentInfoImpl ) o;

        if( !project.equals( that.project ) ) {
            return false;
        }

        //noinspection RedundantIfStatement
        if( !moduleName.equals( that.moduleName ) ) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = project.hashCode();
        result = 31 * result + moduleName.hashCode();
        return result;
    }

    private OsgiBundleFacet getOsgiFacet() {
        Module module = getModule();
        if( module != null ) {
            return OsgiBundleFacet.getOsgiBundleFacet( module );
        } else {
            return null;
        }
    }
}
