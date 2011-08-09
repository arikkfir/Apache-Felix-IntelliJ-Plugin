package com.infolinks.idea.plugins.felix.util.idea;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.FacetType;
import org.jetbrains.idea.maven.importing.FacetImporter;
import org.jetbrains.idea.maven.project.MavenProject;

/**
 * @author arik
 */
public abstract class AbstractFacetImporter<FACET_TYPE extends Facet, FACET_CONFIG_TYPE extends FacetConfiguration, FACET_TYPE_TYPE extends FacetType<FACET_TYPE, FACET_CONFIG_TYPE>>
        extends FacetImporter<FACET_TYPE, FACET_CONFIG_TYPE, FACET_TYPE_TYPE> {

    @SuppressWarnings( { "SameParameterValue" } )
    protected AbstractFacetImporter( String pluginGroupID,
                                     String pluginArtifactID,
                                     FACET_TYPE_TYPE facet_type_type,
                                     String defaultFacetName ) {
        super( pluginGroupID, pluginArtifactID, facet_type_type, defaultFacetName );
    }

    @Override
    protected void setupFacet( FACET_TYPE f, MavenProject mavenProject ) {
    }
}
