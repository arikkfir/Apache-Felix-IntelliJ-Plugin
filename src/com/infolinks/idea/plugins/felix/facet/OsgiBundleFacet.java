package com.infolinks.idea.plugins.felix.facet;

import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Constants;
import com.infolinks.idea.plugins.felix.build.BundleInstructionsHelper;
import com.infolinks.idea.plugins.felix.facet.pkg.BundlePackage;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.codehaus.plexus.util.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;

import static com.intellij.openapi.compiler.CompilerMessageCategory.ERROR;
import static com.intellij.openapi.compiler.CompilerMessageCategory.WARNING;
import static java.util.Collections.emptyList;

/**
 * @author arik
 */
public class OsgiBundleFacet extends Facet<OsgiBundleFacetConfiguration> {

    public static OsgiBundleFacet getOsgiBundleFacet( Module module ) {
        return FacetManager.getInstance( module ).getFacetByType( OsgiBundleFacetType.TYPE_ID );
    }

    private final Logger logger = Logger.getInstance( "#" + getClass().getName() );

    private Builder builder;

    public OsgiBundleFacet( @NotNull FacetType facetType,
                            @NotNull Module module,
                            @NotNull String name,
                            @NotNull OsgiBundleFacetConfiguration configuration,
                            Facet underlyingFacet ) {
        super( facetType, module, name, configuration, underlyingFacet );
    }

    public String getBundleName() {
        return findBundleHeader( Constants.BUNDLE_NAME, getModule().getName() );
    }

    public String getBundleSymbolicName() {
        return findBundleHeader( Constants.BUNDLE_SYMBOLICNAME, getModule().getName() );
    }

    public String getBundleVersion() {
        return findBundleHeader( Constants.BUNDLE_VERSION, "(unknown)" );
    }

    public List<BundlePackage> getExportedPackages() {
        //TODO arik (12/29/10): implement
        return emptyList();
    }

    public List<BundlePackage> getImportedPackages() {
        //TODO arik (12/29/10): implement
        return emptyList();
    }

    public void make( CompileContext context ) {
        refresh();

        Module module = getModule();
        try {
            getBuilder().build();

            List<String> errors = builder.getErrors();
            if( !errors.isEmpty() ) {
                for( String error : errors ) {
                    context.addMessage( ERROR, "Error bundling '" + module.getName() + "': " + error, null, -1, -1 );
                }
                return;
            }

            for( String warning : builder.getWarnings() ) {
                context.addMessage( WARNING, "Warning for module '" + module.getName() + "': " + warning, null, -1, -1 );
            }

            BundleInstructionsHelper helper = BundleInstructionsHelper.getInstance( module );
            if( helper == null ) {
                throw new IllegalStateException( "Module '" + getModule().getName() + "' is not a Maven module! (Maven OSGi facet associated with a non-Maven project)" );
            }
            MavenProject mavenProject = helper.getMavenProject();
            File buildDir = new File( mavenProject.getBuildDirectory() );
            FileUtils.forceMkdir( buildDir );

            String jarFileName = mavenProject.getFinalName() + ".jar";
            File jarFile = new File( buildDir, jarFileName );
            builder.getJar().write( jarFile );

        } catch( Exception e ) {
            String message = "unexpected error occured while bundling '" + module.getName() + "': " + e.getMessage();
            this.logger.error( message, e );
            context.addMessage( ERROR, message, null, -1, -1 );
        }
    }

    public void refresh() {
        if( this.builder != null ) {
            try {
                this.builder.close();
            } catch( Exception e ) {
                this.logger.warn( "Could not close previous JAR builder for module '" + getModule().getName() + "': " + e.getMessage(), e );
            }
        }
        this.builder = null;
    }

    private Builder getBuilder() {
        if( this.builder == null ) {
            try {
                BundleInstructionsHelper helper = BundleInstructionsHelper.getInstance( getModule() );
                if( helper == null ) {
                    throw new IllegalStateException( "Could not create JAR builder for module '" + getModule().getName() + "': module is not a Maven bundle module " );
                } else {
                    this.builder = helper.createJarBuilder();
                }
            } catch( IOException e ) {
                throw new IllegalStateException( "Could not create JAR builder for module '" + getModule().getName() + "': " + e.getMessage(), e );
            }
        }
        return this.builder;
    }

    private String findBundleHeader( String headerName, String defaultValue ) {
        String headerValue = getBuilder().getProperty( headerName );
        if( headerValue != null ) {
            return headerValue;
        } else {
            return defaultValue;
        }
    }
}
