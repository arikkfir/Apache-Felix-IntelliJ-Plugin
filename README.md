Apache Felix IntelliJ Plugin
============================

This plugin provides native IntelliJ IDEA support for developing, running and debugging OSGi projects developed using Apache Felix and the Apache Felix Maven Bundle Plugin.

Using this plugin, developers can run and debug Apache Felix OSGi containers, allowing the user to choose bundles and modules for deployment. The developer can opt for deploying some of the bundles using hot-deploy mechanism (via Apache Felix File Install bundle), allowing for redeployment when bundles are rebuilt, utilizing OSGi's dynamic nature.

The plugin provides a new type of run configuration: Apache Felix run configuration. The configuration of this run configuration allows the developer to choose which bundles to deploy, and the method of deployment (startup or hot-deploy). The list of available bundles for deployment is comprised of all modules with packaging of "bundle", and their dependencies. Non-Maven modules (or Maven modules not of packaging "bundle") are ignored.
