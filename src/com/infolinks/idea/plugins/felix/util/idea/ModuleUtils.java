package com.infolinks.idea.plugins.felix.util.idea;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.File;

/**
 * @author arik
 */
public abstract class ModuleUtils {

    public static File[] getSourceRoots( Module module ) {
        VirtualFile[] vSourceRoots = ModuleRootManager.getInstance( module ).getSourceRoots( false );
        File[] ioSourceRoots = new File[ vSourceRoots.length ];
        for( int i = 0, vSourceRootsLength = vSourceRoots.length; i < vSourceRootsLength; i++ ) {
            ioSourceRoots[ i ] = VfsUtil.virtualToIoFile( vSourceRoots[ i ] );
        }
        return ioSourceRoots;
    }


}
