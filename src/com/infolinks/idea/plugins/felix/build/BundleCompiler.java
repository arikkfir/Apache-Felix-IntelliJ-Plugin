package com.infolinks.idea.plugins.felix.build;

import com.infolinks.idea.plugins.felix.facet.OsgiBundleFacet;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.DataInput;
import java.io.IOException;
import java.util.*;
import org.jetbrains.annotations.NotNull;

import static com.infolinks.idea.plugins.felix.facet.OsgiBundleFacet.getOsgiBundleFacet;

/**
 * @author arik
 */
public class BundleCompiler implements ClassPostProcessingCompiler {

    public static final Key<Set<Module>> MODIFIED_MODULES_KEY = Key.create( "modules" );

    private final Logger logger = Logger.getInstance( "#" + getClass().getName() );

    @NotNull
    public String getDescription() {
        return "OSGi Manifest Compiler";
    }

    public boolean validateConfiguration( CompileScope scope ) {
        return true;
    }

    public ValidityState createValidityState( DataInput in ) throws IOException {
        return TimestampValidityState.load( in );
    }

    @NotNull
    public ProcessingItem[] getProcessingItems( final CompileContext context ) {
        final List<ProcessingItem> processingItems = new LinkedList<ProcessingItem>();
        final CompileScope compileScope = context.isRebuild()
                                          ? context.getProjectCompileScope()
                                          : context.getCompileScope();

        return new ReadAction<ProcessingItem[]>() {

            @Override
            protected void run( Result<ProcessingItem[]> result ) throws Throwable {
                result.setResult( ProcessingItem.EMPTY_ARRAY );

                for( Module module : compileScope.getAffectedModules() ) {
                    BundleInstructionsHelper helper = BundleInstructionsHelper.getInstance( module );
                    if( helper != null ) {
                        OsgiBundleFacet bundleFacet = getOsgiBundleFacet( module );
                        if( bundleFacet != null ) {
                            collectClasses( processingItems, module, helper.getMavenProject().getFile() );
                            collectClasses( processingItems, module, CompilerPaths.getModuleOutputDirectory( module, false ) );
                        }
                    }
                }

                //TODO arik (12/28/10): collect deleted files as processing items

                result.setResult( processingItems.toArray( new ProcessingItem[ processingItems.size() ] ) );
            }
        }.execute().getResultObject();
    }

    public ProcessingItem[] process( CompileContext context, ProcessingItem[] items ) {
        context.getProgressIndicator().setText( "Generating OSGi bundles..." );
        this.logger.info( "Generating OSGi bundles..." );

        List<ProcessingItem> successfulyProcessedItems = new LinkedList<ProcessingItem>();
        Map<Module, Boolean> processedModules = new HashMap<Module, Boolean>();
        Set<Module> updatedModules = new HashSet<Module>();

        for( ProcessingItem processingItem : items ) {
            ClassProcessingItem item = ( ClassProcessingItem ) processingItem;
            Module module = item.module;

            if( processedModules.containsKey( module ) ) {
                if( processedModules.get( module ) ) {
                    successfulyProcessedItems.add( processingItem );
                }
            } else {
                if( processModule( context, module ) ) {
                    processedModules.put( module, true );
                    updatedModules.add( module );
                    successfulyProcessedItems.add( processingItem );
                } else {
                    processedModules.put( module, false );
                }
            }
        }

        context.putUserData( MODIFIED_MODULES_KEY, updatedModules );

        return successfulyProcessedItems.toArray( new ProcessingItem[ successfulyProcessedItems.size() ] );
    }

    private void collectClasses( List<ProcessingItem> processingItems, Module module, VirtualFile file ) {
        if( file != null && file.isValid() ) {
            if( file.isDirectory() ) {
                for( VirtualFile child : file.getChildren() ) {
                    collectClasses( processingItems, module, child );
                }
            } else {
                processingItems.add( new ClassProcessingItem( module, file ) );
            }
        }
    }

    private boolean processModule( CompileContext context, Module module ) {
        String bundleName = getOsgiBundleFacet( module ).getBundleName();
        this.logger.info( "Building OSGi bundle: " + bundleName );
        context.getProgressIndicator().setText2( "Building OSGi bundle: " + bundleName );

        getOsgiBundleFacet( module ).make( context );
        return context.getMessageCount( CompilerMessageCategory.ERROR ) == 0;
    }

    private class ClassProcessingItem implements ProcessingItem {

        private final Module module;

        private final VirtualFile file;

        private ClassProcessingItem( Module module, VirtualFile file ) {
            this.module = module;
            this.file = file;
        }

        @NotNull
        @Override
        public VirtualFile getFile() {
            return this.file;
        }

        @Override
        public ValidityState getValidityState() {
            return new TimestampValidityState( this.file.getModificationStamp() );
        }
    }
}
