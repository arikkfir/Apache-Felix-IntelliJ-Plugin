package com.infolinks.idea.plugins.felix.util.maven;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom.Element;
import org.jetbrains.idea.maven.project.MavenProject;

import static com.infolinks.idea.plugins.felix.facet.OsgiBundleFacetImporter.MAVEN_BUNDLE_PLUGIN_ARTIFACT_ID;
import static com.infolinks.idea.plugins.felix.facet.OsgiBundleFacetImporter.MAVEN_BUNDLE_PLUGIN_GROUP_ID;

/**
 * @author arik
 */
public class Maven2OsgiConverter {

    private static final Pattern FUZZY_VERSION = Pattern.compile( "(\\d+)(\\.(\\d+)(\\.(\\d+))?)?([^a-zA-Z0-9](.*))?", Pattern.DOTALL );

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

    public static String getVersion( String version ) {
        return cleanupVersion( version );
    }

    public static Map<String, String> getPomInstructions( MavenProject mavenProject ) {
        Map<String, String> instructions = new HashMap<String, String>();

        Element pluginConfiguration = mavenProject.getPluginConfiguration( MAVEN_BUNDLE_PLUGIN_GROUP_ID, MAVEN_BUNDLE_PLUGIN_ARTIFACT_ID );
        instructions.putAll( extractInstructionsFromConfigurationElement( pluginConfiguration ) );

        Element goalConfiguration = mavenProject.getPluginGoalConfiguration( MAVEN_BUNDLE_PLUGIN_GROUP_ID, MAVEN_BUNDLE_PLUGIN_ARTIFACT_ID, "manifest" );
        instructions.putAll( extractInstructionsFromConfigurationElement( goalConfiguration ) );

        return transformDirectives( instructions );
    }

    private static Map<String, String> extractInstructionsFromConfigurationElement( Element element ) {
        Map<String, String> instructions = new HashMap<String, String>();

        if( element != null ) {
            Element instructionsContainerElement = element.getChild( "instructions" );
            if( instructionsContainerElement != null ) {

                @SuppressWarnings( { "unchecked" } )
                List<Element> instructionsContainerChildren = instructionsContainerElement.getChildren();

                for( Element instructionElement : instructionsContainerChildren ) {
                    instructions.put( instructionElement.getName(), instructionElement.getText() );
                }
            }
        }

        return instructions;
    }

    private static Map<String, String> transformDirectives( Map<String, String> originalInstructions ) {
        Map<String, String> transformedInstructions = new LinkedHashMap<String, String>();
        for( Object o : originalInstructions.entrySet() ) {
            Map.Entry e = ( Map.Entry ) o;

            String key = ( String ) e.getKey();
            if( key.startsWith( "_" ) ) {
                key = "-" + key.substring( 1 );
            }

            String value = ( String ) e.getValue();
            if( null == value ) {
                value = "";
            } else {
                value = value.replaceAll( "\\p{Blank}*[\r\n]\\p{Blank}*", "" );
            }

            transformedInstructions.put( key, value );
        }
        return transformedInstructions;
    }

    private static String cleanupVersion( String version ) {
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

    private static void cleanupModifier( StringBuffer result, String modifier ) {
        for( int i = 0; i < modifier.length(); i++ ) {
            char c = modifier.charAt( i );
            if( ( c >= '0' && c <= '9' ) || ( c >= 'a' && c <= 'z' ) || ( c >= 'A' && c <= 'Z' ) || c == '_' || c == '-' ) {
                result.append( c );
            } else {
                result.append( '_' );
            }
        }
    }

}
