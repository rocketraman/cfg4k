package com.jdiazcano.cfg4k.loaders

import com.jdiazcano.cfg4k.core.toConfig
import io.kotlintest.Spec
import io.kotlintest.TestCase
import io.kotlintest.TestResult
import io.kotlintest.extensions.TopLevelTest
import io.kotlintest.matchers.types.shouldBeNull
import io.kotlintest.specs.StringSpec
import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.util.Properties

class SystemPropertyConfigLoaderTest : StringSpec() {
    object TestPropertyProvider : PropertyProvider {
        var value1 = "1"
        var value2 = "2"
        override val properties: Properties
            get() {
                return mapOf(
                    "properties.groupone.keyone" to value1,
                    "properties.groupone.keytwo" to value2,
                ).toProperties()
            }
    }

    init {
        val loader by lazy { SystemPropertyConfigLoader(TestPropertyProvider) }

        "it should be good in the loader" {
            loader.get("properties.groupone.keyone").shouldBe("1".toConfig())
        }

        "null if not found" {
            loader.get("this.is.not.found").shouldBeNull()
        }

        "updated when reloading" {
            loader.get("properties.groupone.keyone") shouldBe "1".toConfig()
            TestPropertyProvider.value1 = "11"
            TestPropertyProvider.value2 = "22"
            loader.reload()
            loader.get("properties.groupone.keyone") shouldBe "11".toConfig()
        }
    }
}
