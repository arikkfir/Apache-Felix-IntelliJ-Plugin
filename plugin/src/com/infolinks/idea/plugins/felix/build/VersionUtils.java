package com.infolinks.idea.plugins.felix.build;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author arik
 */
public final class VersionUtils {

    public static final Pattern FUZZY_VERSION = Pattern.compile( "(\\d+)(\\.(\\d+)(\\.(\\d+))?)?([^a-zA-Z0-9](.*))?", Pattern.DOTALL );

    public static String getBundleSymbolicName( String groupId, String artifactIdParam ) {
        int i = groupId.lastIndexOf( '.' );
        String lastSection = groupId.substring( ++i );
        if( artifactIdParam.equals( lastSection ) ) {
            return groupId;
        }

        if( artifactIdParam.startsWith( lastSection ) ) {
            String artifactId = artifactIdParam.substring( lastSection.length() );
            if( Character.isLetterOrDigit( artifactId.charAt( 0 ) ) ) {
                return groupId + "." + artifactId;
            } else {
                return groupId + "." + artifactId.substring( 1 );
            }
        }
        return groupId + "." + artifactIdParam;
    }

    public static String cleanupVersion( String version ) {
        StringBuffer result = new StringBuffer();
        Matcher m = FUZZY_VERSION.matcher( version );
        if( m.matches() ) {
            String major = m.group( 1 );
            String minor = m.group( 3 );
            String micro = m.group( 5 );
            String qualifier = m.group( 7 );

            if( major != null ) {
                result.append( major );
                if( minor != null ) {
                    result.append( "." );
                    result.append( minor );
                    if( micro != null ) {
                        result.append( "." );
                        result.append( micro );
                        if( qualifier != null ) {
                            result.append( "." );
                            cleanupModifier( result, qualifier );
                        }
                    } else if( qualifier != null ) {
                        result.append( ".0." );
                        cleanupModifier( result, qualifier );
                    } else {
                        result.append( ".0" );
                    }
                } else if( qualifier != null ) {
                    result.append( ".0.0." );
                    cleanupModifier( result, qualifier );
                } else {
                    result.append( ".0.0" );
                }
            }
        } else {
            result.append( "0.0.0." );
            cleanupModifier( result, version );
        }
        return result.toString();
    }

    public static void cleanupModifier( StringBuffer result, String modifier ) {
        for( int i = 0; i < modifier.length(); i++ ) {
            char c = modifier.charAt( i );
            if( ( c >= '0' && c <= '9' ) || ( c >= 'a' && c <= 'z' ) || ( c >= 'A' && c <= 'Z' ) || c == '_' || c == '-' ) {
                result.append( c );
            } else {
                result.append( '_' );
            }
        }
    }

    private VersionUtils() {
    }
}
