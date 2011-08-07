package com.infolinks.idea.plugins.felix.build;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Constants;
import aQute.lib.osgi.Jar;
import aQute.lib.spring.SpringXMLType;
import com.infolinks.idea.plugins.felix.util.idea.ModuleUtils;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleComponent;
import com.intellij.openapi.project.Project;
import java.io.File;
import java.io.IOException;
import java.util.*;
import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.model.MavenResource;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import static com.infolinks.idea.plugins.felix.facet.OsgiBundleFacetImporter.FELIX_GROUP_ID;
import static com.infolinks.idea.plugins.felix.facet.OsgiBundleFacetImporter.MAVEN_BUNDLE_PLUGIN_ARTIFACT_ID;
import static com.intellij.openapi.vfs.VfsUtil.virtualToIoFile;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.join;
import static org.codehaus.plexus.util.StringUtils.replace;

/**
 * @author arik
 */
public class BundleInstructionsHelper implements ModuleComponent {

    private static final String MAVEN_SYMBOLICNAME = "maven-symbolicname";

    private static final String MAVEN_RESOURCES = "{maven-resources}";

    private static final String LOCAL_PACKAGES = "{local-packages}";

    @Nullable
    public static BundleInstructionsHelper getInstance( @NotNull Module module ) {
        return module.getComponent( BundleInstructionsHelper.class );
    }

    @NotNull
    private Module module;

    public BundleInstructionsHelper( @NotNull Module module ) {
        this.module = module;
    }

    @Override
    public void projectOpened() {
        //no-op
    }

    @Override
    public void projectClosed() {
        //no-op
    }

    @Override
    public void moduleAdded() {
        //no-op
    }

    @Override
    public void initComponent() {
        //no-op
    }

    @Override
    public void disposeComponent() {
        //no-op
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "BundleInstructionsHelper";
    }

    @NotNull
    public MavenProject getMavenProject() {
        MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance( this.module.getProject() );
        MavenProject mavenProject = mavenProjectsManager.findProject( this.module );
        if( mavenProject == null ) {
            throw new IllegalStateException( "Module '" + module.getName() + "' is not a Maven module! (this should not happen)" );
        } else {
            return mavenProject;
        }
    }

    public Builder createJarBuilder() throws IOException {
        MavenProject mavenProject = getMavenProject();

        Properties properties = new Properties();
        properties.putAll( getGeneralBundleInstructions() );
        properties.putAll( getManualInstructionsFromPom() );

        Builder builder = new Builder();
        builder.setBase( virtualToIoFile( mavenProject.getDirectoryFile() ) );
        builder.setProperties( properties );
        builder.setClasspath( getClasspath() );

        // update BND instructions to add included Maven resources
        addMavenResources( builder );

        // calculate default export/private settings based on sources
        addLocalPackages( builder );

        // embed dependencies if requested to
        //TODO: embedDependencies( builder );

        File outputDirectory = new File( mavenProject.getOutputDirectory() );
        FileUtils.forceMkdir( outputDirectory );
        builder.setJar( outputDirectory );
        return builder;
    }

    @NotNull
    public Properties getGeneralBundleInstructions() {
        MavenProject mavenProject = getMavenProject();
        MavenId mavenId = mavenProject.getMavenId();
        String bsn = VersionUtils.getBundleSymbolicName( mavenId.getGroupId(), mavenId.getArtifactId() );

        Properties properties = new Properties();
        List<MavenResource> resources = mavenProject.getResources();
        if( resources.size() == 1 ) {
            properties.put( "Bundle-LocalResourcesRoot", "file://" + resources.get( 0 ).getDirectory() + "/" );
        }
        properties.put( MAVEN_SYMBOLICNAME, bsn );
        properties.put( Analyzer.BUNDLE_SYMBOLICNAME, bsn );
        properties.put( Analyzer.IMPORT_PACKAGE, "*" );
        properties.put( Analyzer.BUNDLE_VERSION, VersionUtils.cleanupVersion( mavenId.getVersion() ) );
        properties.put( Constants.REMOVEHEADERS, Analyzer.INCLUDE_RESOURCE + ',' + Analyzer.PRIVATE_PACKAGE );

        properties.put( Analyzer.BUNDLE_DESCRIPTION, bsn + " (Maven project description not available inside IntelliJ)" );
        HeaderUtils.header( properties, Analyzer.BUNDLE_NAME, mavenProject.getDisplayName() );

        HeaderUtils.header( properties, Analyzer.BUNDLE_VENDOR, "Unknown (Organization info is not available inside IntelliJ)" );
        properties.put( "project.organization.name", "Unknown (Organization info is not available inside IntelliJ)" );
        properties.put( "pom.organization.name", "Unknown (Organization info is not available inside IntelliJ)" );

        properties.putAll( mavenProject.getProperties() );

        properties.put( "project.baseDir", mavenProject.getDirectory() );
        properties.put( "project.build.directory", mavenProject.getBuildDirectory() );
        properties.put( "project.build.outputdirectory", mavenProject.getOutputDirectory() );

        HeaderUtils.header( properties, Analyzer.PLUGIN, SpringXMLType.class.getName() );

        return properties;
    }

    @NotNull
    public Map<String, String> getManualInstructionsFromPom() {
        Map<String, String> instructions = new HashMap<String, String>();
        MavenProject mavenProject = getMavenProject();

        Element pluginConfiguration = mavenProject.getPluginConfiguration( FELIX_GROUP_ID, MAVEN_BUNDLE_PLUGIN_ARTIFACT_ID );
        if( pluginConfiguration != null ) {
            instructions.putAll( extractInstructionsFromConfigurationElement( pluginConfiguration ) );
        }

        Element goalConfiguration = mavenProject.getPluginGoalConfiguration( FELIX_GROUP_ID, MAVEN_BUNDLE_PLUGIN_ARTIFACT_ID, "manifest" );
        if( goalConfiguration != null ) {
            instructions.putAll( extractInstructionsFromConfigurationElement( goalConfiguration ) );
        }

        return instructions;
    }

    @NotNull
    public Jar[] getClasspath() throws IOException {
        MavenProject mavenProject = getMavenProject();

        List<Jar> list = new ArrayList<Jar>();

        File outputDirectory = new File( mavenProject.getOutputDirectory() );
        if( outputDirectory.exists() ) {
            list.add( new Jar( ".", outputDirectory ) );
        }

        Project project = this.module.getProject();
        MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance( project );

        for( MavenArtifact dependency : mavenProject.getDependencies() ) {
            if( dependency.getType().equalsIgnoreCase( "jar" ) || dependency.getType().equalsIgnoreCase( "bundle" ) ) {
                if( !dependency.getScope().equalsIgnoreCase( Artifact.SCOPE_TEST ) ) {
                    File file;

                    MavenProject dependencyMavenProject = mavenProjectsManager.findProject( dependency );
                    if( dependencyMavenProject != null ) {
                        file = new File( dependencyMavenProject.getOutputDirectory() );
                    } else {
                        file = dependency.getFile();
                    }

                    if( file.exists() ) {
                        Jar jar = new Jar( file );
                        list.add( jar );
                    }
                }
            }
        }

        return list.toArray( new Jar[ list.size() ] );
    }

    public void addMavenResources( @NotNull Analyzer analyzer ) {
        //
        // aggregate all maven resource paths into a single comma-delimited string
        //
        MavenProject mavenProject = getMavenProject();
        Set<String> pathSet = new LinkedHashSet<String>();
        for( MavenResource resource : mavenProject.getResources() ) {
            //
            // ignore non-existant resources
            //
            if( new File( resource.getDirectory() ).exists() ) {
                //
                // ignore empty or non-local resources
                //
                if( resource.getTargetPath() == null || !resource.getTargetPath().contains( ".." ) ) {
                    //
                    // scan the resource definition contents
                    //
                    for( String includedFile : scanMavenResource( resource ) ) {
                        pathSet.add( getIncludedFilePattern( resource, includedFile ) );
                    }
                }
            }
        }
        String mavenResourcePaths = join( pathSet, ',' );

        //
        // now pass single-comma-delimited-string of maven resources onto BND analyzer
        //
        String includeResource = analyzer.getProperty( Analyzer.INCLUDE_RESOURCE );
        if( includeResource != null ) {
            if( includeResource.contains( MAVEN_RESOURCES ) ) {
                // if there is no maven resource path, we do a special treatment and replace
                // every occurance of MAVEN_RESOURCES and a following comma with an empty string
                if( mavenResourcePaths.isEmpty() ) {
                    String cleanedResource = HeaderUtils.removeTagFromInstruction( includeResource, MAVEN_RESOURCES );
                    if( cleanedResource.length() > 0 ) {
                        analyzer.setProperty( Analyzer.INCLUDE_RESOURCE, cleanedResource );
                    } else {
                        analyzer.unsetProperty( Analyzer.INCLUDE_RESOURCE );
                    }
                } else {
                    analyzer.setProperty( Analyzer.INCLUDE_RESOURCE,
                                          replace( includeResource, MAVEN_RESOURCES, mavenResourcePaths ) );
                }
            }
        } else if( mavenResourcePaths.length() > 0 ) {
            analyzer.setProperty( Analyzer.INCLUDE_RESOURCE, mavenResourcePaths );
        }
    }

    public void addLocalPackages( @NotNull Analyzer analyzer ) {
        Collection<String> packages = new LinkedHashSet<String>();

        File outputDirectory = ModuleUtils.getOutputDirectory( this.module );
        if( outputDirectory != null && outputDirectory.isDirectory() ) {
            // scan classes directory for potential packages
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( outputDirectory );
            scanner.setIncludes( new String[] { "**/*.class" } );
            scanner.addDefaultExcludes();
            scanner.scan();
            for( String path : scanner.getIncludedFiles() ) {
                packages.add( HeaderUtils.getPackageName( path ) );
            }
        }

        StringBuilder exportedPkgs = new StringBuilder();
        StringBuilder privatePkgs = new StringBuilder();

        boolean noprivatePackages = "!*".equals( analyzer.getProperty( Analyzer.PRIVATE_PACKAGE ) );
        for( String pkg : packages ) {
            // mark all source packages as private by default (can be overridden by export list)
            privatePkgs.append( pkg ).append( ";-split-package:=merge-first," );

            // we can't export the default package (".") and we shouldn't export internal packages
            if( noprivatePackages || !( ".".equals( pkg ) || pkg.contains( ".internal" ) || pkg.contains( ".impl" ) ) ) {
                if( exportedPkgs.length() > 0 ) {
                    exportedPkgs.append( ';' );
                }
                exportedPkgs.append( pkg );
            }
        }

        if( analyzer.getProperty( Analyzer.EXPORT_PACKAGE ) == null ) {
            if( analyzer.getProperty( Analyzer.EXPORT_CONTENTS ) == null ) {
                // no -exportcontents overriding the exports, so use our computed list
                analyzer.setProperty( Analyzer.EXPORT_PACKAGE, exportedPkgs.toString() );
            } else {
                // leave Export-Package empty (but non-null) as we have -exportcontents
                analyzer.setProperty( Analyzer.EXPORT_PACKAGE, "" );
            }
        } else {
            String exported = analyzer.getProperty( Analyzer.EXPORT_PACKAGE );
            if( exported.contains( LOCAL_PACKAGES ) ) {
                String newExported = replace( exported, LOCAL_PACKAGES, exportedPkgs.toString() );
                analyzer.setProperty( Analyzer.EXPORT_PACKAGE, newExported );
            }
        }

        if( analyzer.getProperty( Analyzer.PRIVATE_PACKAGE ) == null ) {
            // if there are really no private packages then use "!*" as this will keep the Bnd Tool happy
            analyzer.setProperty( Analyzer.PRIVATE_PACKAGE, privatePkgs.length() == 0 ? "!*" : privatePkgs.toString() );
        }
    }

    @NotNull
    private Map<String, String> extractInstructionsFromConfigurationElement( @NotNull Element element ) {
        Map<String, String> instructions = new HashMap<String, String>();

        Element instructionsContainerElement = element.getChild( "instructions" );
        if( instructionsContainerElement != null ) {

            @SuppressWarnings( { "unchecked" } )
            List<Element> instructionsContainerChildren = instructionsContainerElement.getChildren();

            for( Element instructionElement : instructionsContainerChildren ) {
                String instruction = instructionElement.getName();
                if( instruction.startsWith( "_" ) ) {
                    instruction = "-" + instruction.substring( 1 );
                }

                String value = instructionElement.getText();
                if( value == null ) {
                    value = "";
                } else {
                    value = value.replaceAll( "\\p{Blank}*[\r\n]\\p{Blank}*", "" );
                }

                instructions.put( instruction, value );
            }
        }

        return instructions;
    }

    private List<String> scanMavenResource( MavenResource resource ) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( resource.getDirectory() );
        if( resource.getIncludes() != null && !resource.getIncludes().isEmpty() ) {
            scanner.setIncludes( resource.getIncludes().toArray( new String[ resource.getIncludes().size() ] ) );
        } else {
            scanner.setIncludes( new String[] { "**/**" } );
        }
        if( resource.getExcludes() != null && !resource.getExcludes().isEmpty() ) {
            scanner.setExcludes( resource.getExcludes().toArray( new String[ resource.getExcludes().size() ] ) );
        }
        scanner.addDefaultExcludes();
        scanner.scan();
        return asList( scanner.getIncludedFiles() );
    }

    private String getIncludedFilePattern( @NotNull MavenResource resource, @NotNull String includedFile ) {
        MavenProject mavenProject = getMavenProject();

        String basePath = mavenProject.getDirectory();
        String name = includedFile.replace( '\\', '/' );
        String path = resource.getDirectory().replace( '\\', '/' ) + '/' + name;

        // make relative to project
        if( path.startsWith( basePath ) ) {
            if( path.length() == basePath.length() ) {
                path = ".";
            } else {
                path = path.substring( basePath.length() + 1 );
            }
        }

        // copy to correct place
        path = name + '=' + path;
        if( resource.getTargetPath() != null ) {
            path = resource.getTargetPath() + '/' + path;
        }

        // use Bnd filtering?
        if( resource.isFiltered() ) {
            path = '{' + path + '}';
        }
        return path;
    }
}
