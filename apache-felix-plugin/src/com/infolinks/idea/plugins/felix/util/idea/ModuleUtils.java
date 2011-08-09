package com.infolinks.idea.plugins.felix.util.idea;

import com.intellij.openapi.compiler.ex.CompilerPathsEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.File;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author arik
 */
public abstract class ModuleUtils {

    @Nullable
    public static File getOutputDirectory( @NotNull Module module ) {
        VirtualFile outputDirectory = CompilerPathsEx.getModuleOutputDirectory( module, false );
        if( outputDirectory == null ) {
            return null;
        } else {
            return VfsUtil.virtualToIoFile( outputDirectory );
        }
    }


}
