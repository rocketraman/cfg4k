package com.jdiazcano.cfg4k.loaders

interface EnvironmentProvider {
  val environment: Map<String, String>
}

object DefaultEnvironmentProvider: EnvironmentProvider {
  override val environment: Map<String, String> = System.getenv()
}
