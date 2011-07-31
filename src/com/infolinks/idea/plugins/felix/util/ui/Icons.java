package com.infolinks.idea.plugins.felix.util.ui;

import com.intellij.openapi.util.IconLoader;
import javax.swing.*;

/**
 * @author arik
 */
public abstract class Icons {

    private static final ClassLoader CLASS_LOADER = Icons.class.getClassLoader();

    public static final Icon MODULE_ICON = IconLoader.findIcon( "/images/ModuleClosed.png", CLASS_LOADER );

    public static final Icon BUNDLE_ICON = IconLoader.findIcon( "/images/bundle.gif", CLASS_LOADER );

    public static final Icon QUESTION_MARK = IconLoader.findIcon( "/images/warning.png", CLASS_LOADER );

    public static final Icon APACHE_FELIX = IconLoader.getIcon( "/images/logo16x16.png" );
}
