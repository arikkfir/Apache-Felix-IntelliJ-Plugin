package com.infolinks.idea.plugins.felix.facet.pkg;

import java.util.ArrayList;
import java.util.List;

/**
 * @author arik
 */
public class ExportedPackage implements BundlePackage, Comparable<BundlePackage> {

    private String name;

    private String version;

    private List<String> uses = new ArrayList<String>();

    @Override
    public int compareTo( BundlePackage o ) {
        return this.name.compareTo( o.getName() );
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    public void setVersion( String version ) {
        this.version = version;
    }

    @Override
    public List<String> getUsedPackages() {
        return this.uses;
    }

    public void setUses( List<String> uses ) {
        this.uses = uses;
    }
}
