package com.jdiazcano.cfg4k.loaders

open class SystemPropertyConfigLoader(
    protected val propertyProvider: PropertyProvider = DefaultPropertyProvider,
) : DefaultConfigLoader(propertyProvider.properties.toConfig()) {
    constructor() : this(DefaultPropertyProvider)

    override fun reload() {
        root = propertyProvider.properties.toConfig()
    }
}
