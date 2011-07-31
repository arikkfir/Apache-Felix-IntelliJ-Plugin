package com.infolinks.idea.plugins.felix.util.maven;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Constants;
import aQute.lib.osgi.Jar;
import aQute.lib.spring.SpringXMLType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import java.io.File;
import java.io.IOException;
import java.util.*;
import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.model.MavenResource;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import static com.infolinks.idea.plugins.felix.util.idea.ModuleUtils.getSourceRoots;
import static com.infolinks.idea.plugins.felix.util.maven.Maven2OsgiConverter.*;
import static com.intellij.openapi.vfs.VfsUtil.virtualToIoFile;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.join;

/**
 * @author arik
 */
public abstract class MavenUtils {

    private static final String MAVEN_SYMBOLICNAME = "maven-symbolicname";

    private static final String MAVEN_RESOURCES = "{maven-resources}";

    private static final String LOCAL_PACKAGES = "{local-packages}";

    public static Builder createJarBuilder( Module module ) throws IOException {
        MavenProject mavenProject = getMavenProject( module );

        Builder builder = new Builder();

        Properties properties = new Properties();
        properties.putAll( getDefaultProperties( mavenProject ) );
        properties.putAll( getPomInstructions( mavenProject ) );

        builder.setBase( virtualToIoFile( mavenProject.getDirectoryFile() ) );
        builder.setProperties( properties );
        builder.setClasspath( getClasspath( module.getProject(), mavenProject ) );

        // calculate default export/private settings based on sources
        addLocalPackages( module, builder );

        // update BND instructions to add included Maven resources
        includeMavenResources( mavenProject, builder );

        File outputDirectory = new File( mavenProject.getOutputDirectory() );
        FileUtils.forceMkdir( outputDirectory );
        builder.setJar( outputDirectory );
        return builder;
    }

    public static MavenProject getMavenProject( Module module ) {
        MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance( module.getProject() );
        return mavenProjectsManager.findProject( module );
    }

    private static Properties getDefaultProperties( MavenProject mavenProject ) {
        MavenId mavenId = mavenProject.getMavenId();
        String bsn = getBundleSymbolicName( mavenId.getGroupId(), mavenId.getArtifactId() );

        Properties properties = new Properties();
        List<MavenResource> resources = mavenProject.getResources();
        if( resources.size() == 1 ) {
            properties.put( "Web-ResourcesRoot", "file://" + resources.get( 0 ).getDirectory() + "/" );
        }
        properties.put( MAVEN_SYMBOLICNAME, bsn );
        properties.put( Analyzer.BUNDLE_SYMBOLICNAME, bsn );
        properties.put( Analyzer.IMPORT_PACKAGE, "*" );
        properties.put( Analyzer.BUNDLE_VERSION, getVersion( mavenId.getVersion() ) );
        properties.put( Constants.REMOVEHEADERS, Analyzer.INCLUDE_RESOURCE + ',' + Analyzer.PRIVATE_PACKAGE );
        properties.put( "project.baseDir", mavenProject.getDirectory() );
        properties.put( "project.build.directory", mavenProject.getBuildDirectory() );
        properties.put( "project.build.outputdirectory", mavenProject.getOutputDirectory() );
        properties.putAll( mavenProject.getProperties() );

        header( properties, Analyzer.BUNDLE_NAME, mavenProject.getDisplayName() );
        header( properties, Analyzer.PLUGIN, SpringXMLType.class.getName() );

        return properties;
    }

    private static Jar[] getClasspath( Project project, MavenProject mavenProject ) throws IOException {

        List<Jar> list = new ArrayList<Jar>();

        File outputDirectory = new File( mavenProject.getOutputDirectory() );
        if( outputDirectory.exists() ) {
            list.add( new Jar( ".", outputDirectory ) );
        }

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

    private static void addLocalPackages( Module module, Analyzer analyzer ) {
        Collection<String> packages = new LinkedHashSet<String>();

        for( File sourceDirectory : getSourceRoots( module ) ) {
            if( sourceDirectory != null && sourceDirectory.isDirectory() ) {
                // scan local Java sources for potential packages
                DirectoryScanner scanner = new DirectoryScanner();
                scanner.setBasedir( sourceDirectory );
                scanner.setIncludes( new String[] { "**/*.java" } );

                scanner.addDefaultExcludes();
                scanner.scan();

                for( String path : scanner.getIncludedFiles() ) {
                    packages.add( getPackageName( path ) );
                }
            }
        }

        StringBuffer exportedPkgs = new StringBuffer();
        StringBuffer privatePkgs = new StringBuffer();

        for( String pkg : packages ) {

            // mark all source packages as private by default (can be overridden by export list)
            privatePkgs.append( pkg ).append( ";-split-package:=merge-first," );

            // we can't export the default package (".") and we shouldn't export internal packages
            if( !( ".".equals( pkg ) || pkg.contains( ".internal" ) || pkg.contains( ".impl" ) ) ) {
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
            if( exported.indexOf( LOCAL_PACKAGES ) >= 0 ) {
                String newExported = org.codehaus.plexus.util.StringUtils.replace( exported, LOCAL_PACKAGES, exportedPkgs.toString() );
                analyzer.setProperty( Analyzer.EXPORT_PACKAGE, newExported );

            }
        }

        if( analyzer.getProperty( Analyzer.PRIVATE_PACKAGE ) == null ) {
            // if there are really no private packages then use "!*" as this will keep the Bnd Tool happy
            analyzer.setProperty( Analyzer.PRIVATE_PACKAGE, privatePkgs.length() == 0 ? "!*" : privatePkgs.toString() );
        }
    }

    private static String getMavenResourcePaths( MavenProject mavenProject ) {
        Set<String> pathSet = new LinkedHashSet<String>();
        for( MavenResource resource : mavenProject.getResources() ) {
            //
            // ignore non-existant resources
            //
            if( new File( resource.getDirectory() ).exists() ) {
                //
                // ignore empty or non-local resources
                //
                if( resource.getTargetPath() == null || resource.getTargetPath().indexOf( ".." ) < 0 ) {
                    //
                    // scan the resource definition contents
                    //
                    for( String includedFile : scanMavenResource( resource ) ) {
                        pathSet.add( getIncludedFilePattern( mavenProject, resource, includedFile ) );
                    }
                }
            }
        }
        return join( pathSet, ',' );
    }

    private static String getIncludedFilePattern( MavenProject mavenProject,
                                                  MavenResource resource,
                                                  String includedFile ) {
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

    private static List<String> scanMavenResource( MavenResource resource ) {
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

    private static void includeMavenResources( MavenProject mavenProject, Analyzer analyzer ) {
        // pass maven resource paths onto BND analyzer
        final String mavenResourcePaths = getMavenResourcePaths( mavenProject );
        final String includeResource = analyzer.getProperty( Analyzer.INCLUDE_RESOURCE );
        if( includeResource != null ) {
            if( includeResource.indexOf( MAVEN_RESOURCES ) >= 0 ) {
                // if there is no maven resource path, we do a special treatment and replace
                // every occurance of MAVEN_RESOURCES and a following comma with an empty string
                if( mavenResourcePaths.length() == 0 ) {
                    String cleanedResource = removeTagFromInstruction( includeResource, MAVEN_RESOURCES );
                    if( cleanedResource.length() > 0 ) {
                        analyzer.setProperty( Analyzer.INCLUDE_RESOURCE, cleanedResource );
                    } else {
                        analyzer.unsetProperty( Analyzer.INCLUDE_RESOURCE );
                    }
                } else {
                    String combinedResource = org.codehaus.plexus.util.StringUtils.replace( includeResource, MAVEN_RESOURCES, mavenResourcePaths );
                    analyzer.setProperty( Analyzer.INCLUDE_RESOURCE, combinedResource );
                }
            }
        } else if( mavenResourcePaths.length() > 0 ) {
            analyzer.setProperty( Analyzer.INCLUDE_RESOURCE, mavenResourcePaths );
        }
    }

    private static void header( Properties properties, String key, Object value ) {
        if( value == null ) {
            return;
        }

        if( value instanceof Collection && ( ( Collection ) value ).isEmpty() ) {
            return;
        }

        properties.put( key, value.toString().replaceAll( "[\r\n]", "" ) );
    }

    @SuppressWarnings( { "SameParameterValue" } )
    private static String removeTagFromInstruction( String instruction, String tag ) {
        StringBuffer buf = new StringBuffer();

        String[] clauses = instruction.split( "," );
        for( String clause : clauses ) {
            String trimmedClause = clause.trim();
            if( !tag.equals( trimmedClause ) ) {
                if( buf.length() > 0 ) {
                    buf.append( ',' );
                }
                buf.append( trimmedClause );
            }
        }

        return buf.toString();
    }

    private static String getPackageName( String filename ) {
        int n = filename.lastIndexOf( File.separatorChar );
        return n < 0 ? "." : filename.substring( 0, n ).replace( File.separatorChar, '.' );
    }
}
