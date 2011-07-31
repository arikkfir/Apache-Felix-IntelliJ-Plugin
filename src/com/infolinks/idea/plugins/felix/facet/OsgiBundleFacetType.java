package com.infolinks.idea.plugins.felix.facet;

import com.infolinks.idea.plugins.felix.util.ui.Icons;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import javax.swing.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author arik
 */
public class OsgiBundleFacetType extends FacetType<OsgiBundleFacet, OsgiBundleFacetConfiguration> {

    private static final String TYPE_STRING_ID = "osgiBundleFacet";

    public static final FacetTypeId<OsgiBundleFacet> TYPE_ID = new FacetTypeId<OsgiBundleFacet>( TYPE_STRING_ID );

    public static OsgiBundleFacetType getOsgiBundleFacetType() {
        return FacetType.findInstance( OsgiBundleFacetType.class );
    }

    private static final String JAVA__MODULE_TYPE = "JAVA_MODULE";

    public OsgiBundleFacetType() {
        super( TYPE_ID, TYPE_STRING_ID, "OSGi Bundle" );
    }

    @Override
    public OsgiBundleFacetConfiguration createDefaultConfiguration() {
        return new OsgiBundleFacetConfiguration();
    }

    @Override
    public OsgiBundleFacet createFacet( @NotNull Module module,
                                        String name,
                                        @NotNull OsgiBundleFacetConfiguration configuration,
                                        @Nullable Facet underlyingFacet ) {
        return new OsgiBundleFacet( this, module, name, configuration, underlyingFacet );
    }

    @Override
    public Icon getIcon() {
        return Icons.BUNDLE_ICON;
    }

    @Override
    public boolean isSuitableModuleType( ModuleType moduleType ) {
        ModuleType javaModuleType = ModuleTypeManager.getInstance().findByID( JAVA__MODULE_TYPE );
        return javaModuleType != null && moduleType.equals( javaModuleType );
    }
}
