package com.infolinks.idea.plugins.felix.facet;

import com.infolinks.idea.plugins.felix.util.idea.AbstractFacetImporter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import java.util.List;
import java.util.Map;
import org.jdom.Element;
import org.jetbrains.idea.maven.importing.MavenModifiableModelsProvider;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.project.MavenProjectsTree;

import static com.infolinks.idea.plugins.felix.facet.OsgiBundleFacetType.getOsgiBundleFacetType;

/**
 * @author arik
 */
public class OsgiBundleFacetImporter
    extends AbstractFacetImporter<OsgiBundleFacet, OsgiBundleFacetConfiguration, OsgiBundleFacetType> {

    public static final String FELIX_GROUP_ID = "org.apache.felix";

    public static final String MAVEN_BUNDLE_PLUGIN_ARTIFACT_ID = "maven-bundle-plugin";

    public OsgiBundleFacetImporter() {
        super( FELIX_GROUP_ID, MAVEN_BUNDLE_PLUGIN_ARTIFACT_ID, getOsgiBundleFacetType(), getOsgiBundleFacetType().getDefaultFacetName() );
    }

    @Override
    protected void reimportFacet( MavenModifiableModelsProvider modelsProvider,
                                  Module module,
                                  MavenRootModelAdapter rootModel,
                                  final OsgiBundleFacet facet,
                                  MavenProjectsTree mavenTree,
                                  MavenProject mavenProject,
                                  MavenProjectChanges changes,
                                  Map<MavenProject, String> mavenProjectToModuleName,
                                  List<MavenProjectsProcessorTask> postTasks ) {
        if( facet != null ) {

            Element pluginConfig = getConfig( mavenProject );
            if( pluginConfig == null || !"bundle".equalsIgnoreCase( mavenProject.getPackaging() ) ) {

                //
                // remove the OSGi bundle facet if the module doesn't use "bundle" packaging, or has no "maven-bundle-plugin" configuration
                //
                modelsProvider.getFacetModel( module ).removeFacet( facet );

            } else {

                ApplicationManager.getApplication().invokeLater( new Runnable() {

                    @Override
                    public void run() {
                        facet.refresh();
                    }
                } );
            }
        }
    }
}
