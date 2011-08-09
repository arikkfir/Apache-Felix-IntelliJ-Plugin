package com.infolinks.idea.plugins.felix.build;

import java.io.File;
import java.util.Collection;
import java.util.Properties;

/**
 * @author arik
 */
public abstract class HeaderUtils {

    public static String removeTagFromInstruction( String instruction, String tag ) {
        StringBuilder buf = new StringBuilder();

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

    public static String getPackageName( String filename ) {
        int n = filename.lastIndexOf( File.separatorChar );
        return n < 0 ? "." : filename.substring( 0, n ).replace( File.separatorChar, '.' );
    }

    public static void header( Properties properties, String key, Object value ) {
        if( value == null ) {
            return;
        }

        if( value instanceof Collection && ( ( Collection ) value ).isEmpty() ) {
            return;
        }

        properties.put( key, value.toString().replaceAll( "[\r\n]", "" ) );
    }

    private HeaderUtils() {
    }
}
