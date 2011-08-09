package com.infolinks.idea.plugins.felix.runner.ui;

import com.infolinks.idea.plugins.felix.runner.bundle.BundleInfoManager;
import com.infolinks.idea.plugins.felix.runner.FelixRunConfiguration;
import com.infolinks.idea.plugins.felix.runner.deploy.BundleDeploymentInfo;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.table.TableView;
import com.intellij.util.containers.SortedList;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author arik
 */
public class FelixDeploymentSettingsEditor extends SettingsEditor<FelixRunConfiguration> {

    private static final ReverseIndexComparator REVERSE_INDEX_COMPARATOR = new ReverseIndexComparator();

    private AvailableBundlesTable availableBundlesTable;

    private SelectedBundlesTable selectedBundlesTable;

    private JButton undeployButton;

    private JButton deployButton;

    private JPanel root;

    public FelixDeploymentSettingsEditor() {
        this.deployButton.addActionListener( new ActionListener() {

            @Override
            public void actionPerformed( ActionEvent e ) {
                addSelectedBundles();
            }
        } );
        this.undeployButton.addActionListener( new ActionListener() {

            @Override
            public void actionPerformed( ActionEvent e ) {
                removeSelectedBundles();
            }
        } );
    }

    @Override
    protected void resetEditorFrom( FelixRunConfiguration configuration ) {
        this.selectedBundlesTable.getListTableModel().setItems( configuration.getBundles() );
        this.availableBundlesTable.getListTableModel().setItems(
            getAvailableBundles( configuration.getProject(), this.selectedBundlesTable.getItems() ) );

        ColumnInfo[] columnInfos = this.selectedBundlesTable.getListTableModel().getColumnInfos();
        for( ColumnInfo columnInfo : columnInfos ) {
            if( columnInfo instanceof RunConfigurationAware ) {
                RunConfigurationAware aware = ( RunConfigurationAware ) columnInfo;
                aware.setRunConfiguration( configuration );
            }
        }
    }

    @Override
    protected void applyEditorTo( FelixRunConfiguration configuration ) throws ConfigurationException {
        configuration.setBundles( this.selectedBundlesTable.getItems() );
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return this.root;
    }

    @Override
    protected void disposeEditor() {
        //no-op
    }

    private void addSelectedBundles() {
        switchRows( this.availableBundlesTable, this.selectedBundlesTable );
    }

    private void removeSelectedBundles() {
        switchRows( this.selectedBundlesTable, this.availableBundlesTable );
    }

    private void switchRows( TableView<BundleDeploymentInfo> t1, TableView<BundleDeploymentInfo> t2 ) {
        SortedList<Integer> indicesToRemove = new SortedList<Integer>( REVERSE_INDEX_COMPARATOR );

        ListTableModel<BundleDeploymentInfo> availableBundlesModel = t1.getListTableModel();
        for( int selectionIndex : t1.getSelectedRows() ) {
            int modelIndex = t1.convertRowIndexToModel( selectionIndex );
            indicesToRemove.add( modelIndex );
            BundleDeploymentInfo item = ( BundleDeploymentInfo ) availableBundlesModel.getItem( modelIndex );
            t2.getListTableModel().addRow( item );
        }

        for( Integer indice : indicesToRemove ) {
            t1.getListTableModel().removeRow( indice );
        }
    }

    private List<BundleDeploymentInfo> getAvailableBundles( Project project,
                                                            List<BundleDeploymentInfo> selectedBundles ) {
        //TODO arik (7/8/11): move this to the run configuration, cached (to prevent delay, maybe even on project load)

        List<BundleDeploymentInfo> availableBundles = BundleInfoManager.getInstance(project).getAvailableBundles();
        @SuppressWarnings( { "UnnecessaryLocalVariable", "unchecked" } )
        Collection<BundleDeploymentInfo> collection = CollectionUtils.subtract( availableBundles, selectedBundles );
        return new ArrayList<BundleDeploymentInfo>( collection );
    }

    private static class ReverseIndexComparator implements Comparator<Integer> {

        @Override
        public int compare( Integer o1, Integer o2 ) {
            //
            // we multiply the compare-result with -1 so the list will actually be reversed
            return o1.compareTo( o2 ) * -1;
        }
    }
}
