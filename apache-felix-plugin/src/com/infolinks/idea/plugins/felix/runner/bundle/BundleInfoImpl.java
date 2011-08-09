package com.infolinks.idea.plugins.felix.runner.bundle;

import java.io.Serializable;

/**
 * @author arik
 */
public class BundleInfoImpl implements BundleInfo, Serializable {

    private String filename;

    private String name;

    private String symbolicName;

    private String osgiVersion;

    public String getFilename() {
        return filename;
    }

    public void setFilename( String filename ) {
        this.filename = filename;
    }

    @Override
    public String getName() {
        if( this.name == null || this.name.trim().length() == 0 ) {
            return getSymbolicName();
        } else {
            return this.name;
        }
    }

    public void setName( String name ) {
        this.name = name;
    }

    @Override
    public String getSymbolicName() {
        if( this.symbolicName == null || this.symbolicName.trim().length() == 0 ) {
            return this.filename;
        } else {
            return this.symbolicName;
        }
    }

    public void setSymbolicName( String symbolicName ) {
        this.symbolicName = symbolicName;
    }

    @Override
    public String getOsgiVersion() {
        if( this.osgiVersion == null || this.osgiVersion.trim().length() == 0 ) {
            return "(no version)";
        } else {
            return this.osgiVersion;
        }
    }

    public void setOsgiVersion( String osgiVersion ) {
        this.osgiVersion = osgiVersion;
    }
}
