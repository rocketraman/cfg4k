package com.jdiazcano.cfg4k.loaders

import com.jdiazcano.cfg4k.core.toConfig
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class EnvironmentConfigLoaderTest: StringSpec() {
    object TestEnvironmentProvider: EnvironmentProvider {
        var value1 = "1"
        var value2 = "2"
        override val environment: Map<String, String>
            get() {
                return mapOf(
                    "PROPERTIES_GROUPONE_KEYONE" to value1,
                    "PROPERTIES_GROUPONE_KEYTWO" to value2,
                )
            }
    }

    init {
        val loader by lazy {
            EnvironmentConfigLoader(environmentProvider = TestEnvironmentProvider)
         }

        "it should be good in the loader" {
            loader.get("properties.groupone.keyone") shouldBe "1".toConfig()
        }

        "null if not found" {
            loader.get("this.is.not.found").shouldBeNull()
        }

        "updated when reloading" {
            loader.get("properties.groupone.keyone") shouldBe "1".toConfig()
            TestEnvironmentProvider.value1 = "11"
            TestEnvironmentProvider.value2 = "22"
            loader.reload()
            loader.get("properties.groupone.keyone") shouldBe "11".toConfig()
        }
    }
}
