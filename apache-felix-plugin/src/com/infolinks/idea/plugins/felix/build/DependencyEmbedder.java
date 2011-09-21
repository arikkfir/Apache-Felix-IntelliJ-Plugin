/*
 * Copyright (c) 2011 Arik Kfir. All rights reserved.
 */

package com.infolinks.idea.plugins.felix.build;

import aQute.lib.osgi.Analyzer;
import aQute.libg.header.OSGiHeader;
import com.intellij.openapi.compiler.CompileContext;
import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenArtifact;

import static com.infolinks.idea.plugins.felix.build.HeaderUtils.removeTagFromInstruction;
import static com.intellij.openapi.compiler.CompilerMessageCategory.WARNING;

/**
 * @author arik
 */
public final class DependencyEmbedder extends AbstractDependencyFilter {

    public static final String EMBED_DEPENDENCY = "Embed-Dependency";

    public static final String EMBED_DIRECTORY = "Embed-Directory";

    public static final String EMBED_STRIP_GROUP = "Embed-StripGroup";

    public static final String EMBED_STRIP_VERSION = "Embed-StripVersion";

    public static final String EMBED_TRANSITIVE = "Embed-Transitive";

    private static final String MAVEN_DEPENDENCIES = "{maven-dependencies}";

    private String m_embedDirectory;

    private String m_embedStripGroup;

    private String m_embedStripVersion;

    /**
     * Inlined paths.
     */
    private final Collection<String> m_inlinedPaths;

    /**
     * Embedded artifacts.
     */
    private final Collection<MavenArtifact> m_embeddedArtifacts;

    @Nullable
    private CompileContext compileContext;

    public DependencyEmbedder( Collection<MavenArtifact> dependencyArtifacts ) {
        this( dependencyArtifacts, null );
    }

    public DependencyEmbedder( Collection<MavenArtifact> dependencyArtifacts,
                               @Nullable CompileContext compileContext ) {
        super( dependencyArtifacts );
        this.m_inlinedPaths = new LinkedHashSet<String>();
        this.m_embeddedArtifacts = new LinkedHashSet<MavenArtifact>();
        this.compileContext = compileContext;
    }


    public void processHeaders( Analyzer analyzer ) {
        StringBuffer includeResource = new StringBuffer();
        StringBuffer bundleClassPath = new StringBuffer();

        m_inlinedPaths.clear();
        m_embeddedArtifacts.clear();

        String embedDependencyHeader = analyzer.getProperty( EMBED_DEPENDENCY );
        if( null != embedDependencyHeader && embedDependencyHeader.length() > 0 ) {
            m_embedDirectory = analyzer.getProperty( EMBED_DIRECTORY );
            m_embedStripGroup = analyzer.getProperty( EMBED_STRIP_GROUP, "true" );
            m_embedStripVersion = analyzer.getProperty( EMBED_STRIP_VERSION );

            Map<String, Map<String, String>> embedInstructions = OSGiHeader.parseHeader( embedDependencyHeader );
            processInstructions( embedInstructions );

            for( String m_inlinedPath : m_inlinedPaths ) {
                inlineDependency( m_inlinedPath, includeResource );
            }
            for( MavenArtifact m_embeddedArtifact : m_embeddedArtifacts ) {
                embedDependency( m_embeddedArtifact, includeResource, bundleClassPath );
            }
        }

        if( analyzer.getProperty( Analyzer.WAB ) == null && bundleClassPath.length() > 0 ) {
            // set explicit default before merging dependency classpath
            if( analyzer.getProperty( Analyzer.BUNDLE_CLASSPATH ) == null ) {
                analyzer.setProperty( Analyzer.BUNDLE_CLASSPATH, "." );
            }
        }

        appendDependencies( analyzer, Analyzer.INCLUDE_RESOURCE, includeResource.toString() );
        appendDependencies( analyzer, Analyzer.BUNDLE_CLASSPATH, bundleClassPath.toString() );
    }


    @Override
    protected void processDependencies( String tag, String inline, Collection<MavenArtifact> dependencies ) {
        if( dependencies.isEmpty() && this.compileContext != null ) {
            this.compileContext.addMessage(
                    WARNING, EMBED_DEPENDENCY + ": clause \"" + tag + "\" did not match any dependencies", null, -1, -1 );
        }

        if( null == inline || "false".equalsIgnoreCase( inline ) ) {
            m_embeddedArtifacts.addAll( dependencies );
        } else {
            for( MavenArtifact dependency : dependencies ) {
                addInlinedPaths( dependency, inline, m_inlinedPaths );
            }
        }
    }


    private static void addInlinedPaths( MavenArtifact dependency, String inline, Collection<String> inlinedPaths ) {
        File path = dependency.getFile();
        if( path.exists() ) {
            if( "true".equalsIgnoreCase( inline ) || inline.length() == 0 ) {
                inlinedPaths.add( path.getPath() );
            } else {
                String[] filters = inline.split( "\\|" );
                for( String filter : filters ) {
                    if( filter.length() > 0 ) {
                        inlinedPaths.add( path + "!/" + filter );
                    }
                }
            }
        }
    }

    private void embedDependency( MavenArtifact dependency,
                                  StringBuffer includeResource,
                                  StringBuffer bundleClassPath ) {
        File sourceFile = dependency.getFile();
        if( sourceFile.exists() ) {
            String embedDirectory = m_embedDirectory;
            if( "".equals( embedDirectory ) || ".".equals( embedDirectory ) ) {
                embedDirectory = null;
            }

            if( !Boolean.valueOf( m_embedStripGroup ) ) {
                embedDirectory = new File( embedDirectory, dependency.getGroupId() ).getPath();
            }

            File targetFile;
            if( Boolean.valueOf( m_embedStripVersion ) ) {
                targetFile = new File( embedDirectory, dependency.getArtifactId() + ".jar" );
            } else {
                targetFile = new File( embedDirectory, sourceFile.getName() );
            }

            String targetFilePath = targetFile.getPath();

            // replace windows backslash with a slash
            if( File.separatorChar != '/' ) {
                targetFilePath = targetFilePath.replace( File.separatorChar, '/' );
            }

            if( includeResource.length() > 0 ) {
                includeResource.append( ',' );
            }

            includeResource.append( targetFilePath );
            includeResource.append( '=' );
            includeResource.append( sourceFile );

            if( bundleClassPath.length() > 0 ) {
                bundleClassPath.append( ',' );
            }

            bundleClassPath.append( targetFilePath );
        }
    }


    private static void inlineDependency( String path, StringBuffer includeResource ) {
        if( includeResource.length() > 0 ) {
            includeResource.append( ',' );
        }

        includeResource.append( '@' );
        includeResource.append( path );
    }


    public Collection<String> getInlinedPaths() {
        return m_inlinedPaths;
    }


    public Collection<MavenArtifact> getEmbeddedArtifacts() {
        return m_embeddedArtifacts;
    }


    private static void appendDependencies( Analyzer analyzer, String directiveName, String mavenDependencies ) {
        /*
         * similar algorithm to {maven-resources} but default behaviour here is to append rather than override
         */
        final String instruction = analyzer.getProperty( directiveName );
        if( instruction != null && instruction.length() > 0 ) {
            if( instruction.contains( MAVEN_DEPENDENCIES ) ) {
                // if there are no embeddded dependencies, we do a special treatment and replace
                // every occurance of MAVEN_DEPENDENCIES and a following comma with an empty string
                if( mavenDependencies.length() == 0 ) {
                    String cleanInstruction = removeTagFromInstruction( instruction, MAVEN_DEPENDENCIES );
                    analyzer.setProperty( directiveName, cleanInstruction );
                } else {
                    String mergedInstruction = StringUtils.replace( instruction, MAVEN_DEPENDENCIES, mavenDependencies );
                    analyzer.setProperty( directiveName, mergedInstruction );
                }
            } else if( mavenDependencies.length() > 0 ) {
                if( Analyzer.INCLUDE_RESOURCE.equalsIgnoreCase( directiveName ) ) {
                    // dependencies should be prepended so they can be overwritten by local resources
                    analyzer.setProperty( directiveName, mavenDependencies + ',' + instruction );
                } else
                // Analyzer.BUNDLE_CLASSPATH
                {
                    // for the classpath we want dependencies to be appended after local entries
                    analyzer.setProperty( directiveName, instruction + ',' + mavenDependencies );
                }
            }
            // otherwise leave instruction unchanged
        } else if( mavenDependencies.length() > 0 ) {
            analyzer.setProperty( directiveName, mavenDependencies );
        }
        // otherwise leave instruction unchanged
    }
}
