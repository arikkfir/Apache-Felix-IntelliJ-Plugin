package com.infolinks.idea.plugins.felix.facet;

import com.intellij.facet.FacetConfiguration;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import javax.swing.*;
import org.jdom.Element;
import org.jetbrains.annotations.Nls;

import static com.intellij.openapi.components.StorageScheme.DIRECTORY_BASED;

/**
 * @author arik
 */
@State( name = "OsgiBundle",
        storages = {
                @Storage( id = "ipr", file = "$PROJECT_FILE$" ), @Storage( id = "prjDir",
                file = "${PROJECT_CONFIG_DIR$/osgiBundleConfig.xml",
                isDefault = true,
                scheme = DIRECTORY_BASED )
        } )
public class OsgiBundleFacetConfiguration
        implements FacetConfiguration, PersistentStateComponent<OsgiBundleFacetConfiguration.State> {

    public static class State {

    }

    private OsgiBundleFacetConfiguration.State state = new State();

    @Override
    public FacetEditorTab[] createEditorTabs( FacetEditorContext editorContext,
                                              FacetValidatorsManager validatorsManager ) {
        return new FacetEditorTab[] { new OsgiBundleFacetEditorTab( ( OsgiBundleFacet ) editorContext.getFacet() ) };
    }

    @Override
    public void readExternal( Element element ) throws InvalidDataException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeExternal( Element element ) throws WriteExternalException {
        throw new UnsupportedOperationException();
    }

    @Override
    public OsgiBundleFacetConfiguration.State getState() {
        return this.state;
    }

    @Override
    public void loadState( OsgiBundleFacetConfiguration.State state ) {
        this.state = state;
    }

    private class OsgiBundleFacetEditorTab extends FacetEditorTab {

        private final OsgiBundleFacetConfigurationForm form = new OsgiBundleFacetConfigurationForm();

        private final OsgiBundleFacet facet;

        private OsgiBundleFacetEditorTab( OsgiBundleFacet facet ) {
            this.facet = facet;
        }

        @Nls
        @Override
        public String getDisplayName() {
            return "OSGi Bundle";
        }

        @Override
        public JComponent createComponent() {
            return this.form.getRoot();
        }

        @Override
        public boolean isModified() {
            return false;
        }

        @Override
        public void reset() {
            this.form.resetFrom( this.facet );
        }

        @Override
        public void disposeUIResources() {
        }
    }
}
