package com.infolinks.idea.plugins.felix.runner.ui;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AddDeleteListPanel;
import com.intellij.util.Consumer;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.intellij.openapi.fileChooser.FileChooserDescriptorFactory.createSingleFolderDescriptor;

/**
 * @author arik
 */
public class HotDeployDirectoriesPanel extends AddDeleteListPanel<File> {

    private Project project;

    public HotDeployDirectoriesPanel() {
        super( "Hot deployment directories", Collections.<File>emptyList() );
    }

    @Override
    public void setEnabled( boolean enabled ) {
        myAddButton.setEnabled( enabled );
        myDeleteButton.setEnabled( enabled );
        myList.setEnabled( enabled );
    }

    public void setProject( Project project ) {
        this.project = project;
    }

    @Override
    protected File findItemToAdd() {
        final AtomicReference<VirtualFile> ref = new AtomicReference<VirtualFile>( null );

        FileChooser.chooseFilesWithSlideEffect( createSingleFolderDescriptor(), this.project, null, new Consumer<VirtualFile[]>() {

            @Override
            public void consume( VirtualFile[] virtualFiles ) {
                if( virtualFiles != null && virtualFiles.length > 0 ) {
                    ref.set( virtualFiles[ 0 ] );
                }
            }
        } );

        VirtualFile file = ref.get();
        if( file != null ) {
            return VfsUtil.virtualToIoFile( file );
        } else {
            return null;
        }
    }

    public List<File> getDirectories() {
        List<File> items = new ArrayList<File>();
        for( int i = 0; i < myListModel.size(); i++ ) {
            items.add( ( File ) myListModel.getElementAt( i ) );
        }
        return items;
    }

    public void setDirectories( List<File> dirs ) {
        this.myListModel.clear();
        if( dirs != null ) {
            for( File dir : dirs ) {
                addElement( dir );
            }
        }
    }
}
