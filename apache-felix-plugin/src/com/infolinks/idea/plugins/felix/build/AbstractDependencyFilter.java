/*
 * Copyright (c) 2011 Arik Kfir. All rights reserved.
 */

package com.infolinks.idea.plugins.felix.build;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import org.jetbrains.idea.maven.model.MavenArtifact;

/**
 * @author arik
 */
public abstract class AbstractDependencyFilter {

    /**
     * Dependency artifacts.
     */
    private final Collection<MavenArtifact> m_dependencyArtifacts;


    public AbstractDependencyFilter( Collection<MavenArtifact> dependencyArtifacts ) {
        m_dependencyArtifacts = dependencyArtifacts;
    }

    protected final void processInstructions( Map<String, Map<String, String>> instructions ) {
        DependencyFilter filter;
        for( Map.Entry<String, Map<String, String>> entry : instructions.entrySet() ) {
            String inline = "false";

            // must use a fresh *modifiable* collection for each unique clause
            Collection<MavenArtifact> filteredDependencies = new HashSet<MavenArtifact>( m_dependencyArtifacts );

            // CLAUSE: REGEXP --> { ATTRIBUTE MAP }
            StringBuilder tag = new StringBuilder();
            tag.append( entry.getKey() );

            if( !entry.getKey().matches( "\\*~*" ) ) {
                filter = new DependencyFilter( ( String ) entry.getKey() ) {

                    boolean matches( MavenArtifact dependency ) {
                        return super.matches( dependency.getArtifactId() );
                    }
                };
                // FILTER ON MAIN CLAUSE
                filter.filter( filteredDependencies );
            }

            for( Map.Entry<String, String> attr : entry.getValue().entrySet() ) {
                // ATTRIBUTE: KEY --> REGEXP
                tag.append( ';' ).append( attr );

                if( "groupId".equals( attr.getKey() ) ) {
                    filter = new DependencyFilter( ( String ) attr.getValue() ) {

                        boolean matches( MavenArtifact dependency ) {
                            return super.matches( dependency.getGroupId() );
                        }
                    };
                } else if( "artifactId".equals( attr.getKey() ) ) {
                    filter = new DependencyFilter( ( String ) attr.getValue() ) {

                        boolean matches( MavenArtifact dependency ) {
                            return super.matches( dependency.getArtifactId() );
                        }
                    };
                } else if( "version".equals( attr.getKey() ) ) {
                    filter = new DependencyFilter( ( String ) attr.getValue() ) {

                        boolean matches( MavenArtifact dependency ) {
                            return super.matches( dependency.getVersion() );
                        }
                    };
                } else if( "scope".equals( attr.getKey() ) ) {
                    filter = new DependencyFilter( ( String ) attr.getValue(), "compile" ) {

                        boolean matches( MavenArtifact dependency ) {
                            return super.matches( dependency.getScope() );
                        }
                    };
                } else if( "type".equals( attr.getKey() ) ) {
                    filter = new DependencyFilter( ( String ) attr.getValue(), "jar" ) {

                        boolean matches( MavenArtifact dependency ) {
                            return super.matches( dependency.getType() );
                        }
                    };
                } else if( "classifier".equals( attr.getKey() ) ) {
                    filter = new DependencyFilter( ( String ) attr.getValue() ) {

                        boolean matches( MavenArtifact dependency ) {
                            return super.matches( dependency.getClassifier() );
                        }
                    };
                } else if( "optional".equals( attr.getKey() ) ) {
                    filter = new DependencyFilter( ( String ) attr.getValue(), "false" ) {

                        boolean matches( MavenArtifact dependency ) {
                            return super.matches( "" + dependency.isOptional() );
                        }
                    };
                } else if( "inline".equals( attr.getKey() ) ) {
                    inline = attr.getValue();
                    continue;
                } else {
                    throw new IllegalArgumentException( "Unexpected attribute " + attr.getKey() );
                }

                // FILTER ON EACH ATTRIBUTE
                filter.filter( filteredDependencies );
            }

            processDependencies( tag.toString(), inline, filteredDependencies );
        }
    }


    protected abstract void processDependencies( String clause, String inline, Collection<MavenArtifact> dependencies );
}
