package com.jdiazcano.cfg4k.loaders

import java.util.Properties

interface PropertyProvider {
  val properties: Properties
}

object DefaultPropertyProvider: PropertyProvider {
  override val properties: Properties = System.getProperties()
}
