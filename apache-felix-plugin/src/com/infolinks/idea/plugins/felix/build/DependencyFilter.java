/*
 * Copyright (c) 2011 Arik Kfir. All rights reserved.
 */

package com.infolinks.idea.plugins.felix.build;

import aQute.lib.osgi.Instruction;
import java.util.Collection;
import java.util.Iterator;
import org.jetbrains.idea.maven.model.MavenArtifact;

/**
 * @author arik
 */
public abstract class DependencyFilter {

    private final Instruction m_instruction;

    private final String m_defaultValue;


    public DependencyFilter( String expression ) {
        this( expression, "" );
    }


    public DependencyFilter( String expression, String defaultValue ) {
        m_instruction = Instruction.getPattern( expression );
        m_defaultValue = defaultValue;
    }


    public void filter( Collection<MavenArtifact> dependencies ) {
        for( Iterator<MavenArtifact> i = dependencies.iterator(); i.hasNext(); ) {
            if( !matches( i.next() ) ) {
                i.remove();
            }
        }
    }


    abstract boolean matches( MavenArtifact dependency );


    boolean matches( String text ) {
        boolean result;

        if( null == text ) {
            result = m_instruction.matches( m_defaultValue );
        } else {
            result = m_instruction.matches( text );
        }

        return m_instruction.isNegated() ? !result : result;
    }
}
