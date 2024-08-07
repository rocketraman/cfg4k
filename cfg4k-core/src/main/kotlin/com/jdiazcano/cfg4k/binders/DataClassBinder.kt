package com.jdiazcano.cfg4k.binders

import com.jdiazcano.cfg4k.core.ConfigContext
import com.jdiazcano.cfg4k.providers.ConfigProvider
import com.jdiazcano.cfg4k.providers.load
import com.jdiazcano.cfg4k.utils.ConstructorNotFound
import com.jdiazcano.cfg4k.utils.convert
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.jvm.javaType

object DataClassBinder {
    fun <T : Any> bind(configProvider: ConfigProvider, prefix: String, type: KClass<T>): T {
        if (type.visibility == KVisibility.PRIVATE) {
            throw IllegalArgumentException("Binding data classes can't be private: ${type.qualifiedName}")
        }

        val constructors = type.constructors
        val configObject = configProvider.load(prefix) ?: throw IllegalArgumentException("Config object not found: $prefix")

        val paramNames = configObject.asObject().map { it.key }.toSet()

        // find the best matching constructor
        val matchingConstructor = constructors
            .map { c -> c to c.parameters.associateBy { it.name } }
            .filter { (_, cNames) ->
                // any parameters we don't have must be optional or nullable
                (cNames.keys - paramNames).all {
                    cNames[it]?.isOptional ?: false || cNames[it]?.type?.isMarkedNullable ?: false
                }
            }
            .minByOrNull { (_, cNames) -> (cNames.keys - paramNames).size }
            ?.first
            ?: throw ConstructorNotFound("Constructor for class ${type.simpleName} wasn't found. Names: $paramNames")

        val constructorParameters = matchingConstructor.parameters.mapNotNull { parameter ->
            val structure = parameter.type.javaType.convert()
            val context = ConfigContext(configProvider, concatPrefix(prefix, parameter.name!!))
            val subObject = configProvider.load(context)
                ?: if (parameter.type.isMarkedNullable) null
                else if (parameter.isOptional) return@mapNotNull null
                else throw IllegalArgumentException("Parameter ${parameter.name} isn't marked as nullable and value was null.")
            val value = subObject?.let { convert(context, subObject, structure) }
            parameter to value
        }.toMap()

        return matchingConstructor.callBy(constructorParameters)
    }

}
