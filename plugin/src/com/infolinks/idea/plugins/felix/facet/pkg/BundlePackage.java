package com.infolinks.idea.plugins.felix.facet.pkg;

import java.util.List;

/**
 * @author arik
 */
public interface BundlePackage extends Comparable<BundlePackage> {

    String getName();

    String getVersion();

    List<String> getUsedPackages();
}
