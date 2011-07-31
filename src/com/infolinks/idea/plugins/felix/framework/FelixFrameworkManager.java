package com.infolinks.idea.plugins.felix.framework;

import com.infolinks.idea.plugins.felix.util.ui.Icons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import java.io.File;
import javax.swing.*;
import org.apache.commons.lang.ObjectUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * @author arik
 */
@State( name = "felixFrameworks", storages = @Storage( id = "other", file = "$APP_CONFIG$/felixFrameworks.xml" ) )
public class FelixFrameworkManager
        implements ApplicationComponent, Configurable, PersistentStateComponent<FelixFrameworkManager.State> {

    public static FelixFrameworkManager getInstance() {
        return ApplicationManager.getApplication().getComponent( FelixFrameworkManager.class );
    }

    public static class State {

        @SuppressWarnings( { "WeakerAccess" } )
        public String frameworkPath;

    }

    private final FelixFrameworkManagerForm form = new FelixFrameworkManagerForm();

    private State state = new State();

    public File getFrameworkPath() {
        if( this.state.frameworkPath != null ) {
            return new File( this.state.frameworkPath );
        } else {
            return null;
        }
    }

    @NotNull
    public String getComponentName() {
        return FelixFrameworkManager.class.getSimpleName();
    }

    public void initComponent() {
        //no-op
    }

    public void disposeComponent() {
        //no-op
    }

    @Nls
    public String getDisplayName() {
        return "Felix Framework";
    }

    public Icon getIcon() {
        return Icons.APACHE_FELIX;
    }

    public String getHelpTopic() {
        return null;
    }

    public FelixFrameworkManager.State getState() {
        return this.state;
    }

    public void loadState( FelixFrameworkManager.State state ) {
        this.state = state;
    }

    public JComponent createComponent() {
        return this.form.getRoot();
    }

    public boolean isModified() {
        return !ObjectUtils.equals( getFrameworkPath(), this.form.getFrameworkPath() );
    }

    public void apply() throws ConfigurationException {
        File frameworkPath = this.form.getFrameworkPath();
        this.state.frameworkPath = frameworkPath != null ? frameworkPath.getAbsolutePath() : null;
    }

    public void reset() {
        this.form.setFrameworkPath( getFrameworkPath() );
    }

    public void disposeUIResources() {
        //no-op
    }
}
