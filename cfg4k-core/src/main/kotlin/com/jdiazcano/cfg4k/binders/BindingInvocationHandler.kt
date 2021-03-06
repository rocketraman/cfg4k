/*
 * Copyright 2015-2016 Javier Díaz-Cano Martín-Albo (javierdiazcanom@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jdiazcano.cfg4k.binders

import com.jdiazcano.cfg4k.core.ConfigObject
import com.jdiazcano.cfg4k.parsers.Parsers.findParser
import com.jdiazcano.cfg4k.providers.ConfigProvider
import com.jdiazcano.cfg4k.utils.SettingNotFound
import com.jdiazcano.cfg4k.utils.TargetType
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Type
import java.util.LinkedList
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.jvm.kotlinFunction

/**
 * InvocationHandler that handles the proxying between the interface and the call. This class is used in the
 * ProxyConfigProvider.
 */
class BindingInvocationHandler(
        private val provider: ConfigProvider,
        private val prefix: String
) : InvocationHandler {

    private val objectMethods: List<String> = Object::class.java.declaredMethods.map { it.name }

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        val name = getPropertyName(method.name)

        val kotlinClass = method.declaringClass.kotlin
        val isNullable = kotlinClass.isMethodNullable(method, name)

        // If method is toString()/equals() etc, we just return it
        if (objectMethods.contains(method.name)) {
            return method.invoke(this, *(args ?: arrayOf()))
        }

        val type = method.genericReturnType
        val configObject = provider.load(concatPrefix(prefix, name))
        if (configObject == null) {
            try {
                return kotlinClass.getDefaultMethod(method.name)?.invoke(this, proxy)
            } catch (e: Exception) { // There's no default
                if (isNullable) {
                    return null
                } else {
                    throw SettingNotFound("Setting $name not found")
                }
            }
        } else {
            if (configObject.isArray()) {
                val targetType = TargetType(type)
                val rawType = targetType.rawTargetType()
                val collection = createCollection(rawType)
                toMutableCollection(configObject, type, collection, name, provider, prefix)
                return collection
            } else if (configObject.isPrimitive()) {
                val targetType = TargetType(type)
                val rawType = targetType.rawTargetType()
                val superType = targetType.getParameterizedClassArguments().firstOrNull()
                val classType = superType ?: rawType
                return classType.findParser().parse(configObject, classType, superType?.findParser())
            } else { // it is an object
                return provider.bind(concatPrefix(prefix, name), method.returnType)
            }
        }

    }

    override fun equals(other: Any?): Boolean {
        return hashCode() == other?.hashCode()
    }

}

fun createCollection(rawType: Class<*>): MutableCollection<Any?> {
    return if (ArrayList::class.java.isAssignableFrom(rawType)) {
        arrayListOf<Any?>()
    } else if (LinkedList::class.java.isAssignableFrom(rawType)) {
        mutableListOf<Any?>()
    } else if (LinkedHashSet::class.java.isAssignableFrom(rawType)) {
        mutableSetOf<Any?>()
    } else if (HashSet::class.java.isAssignableFrom(rawType)) {
        hashSetOf<Any?>()
    } else if (List::class.java.isAssignableFrom(rawType)) {
        arrayListOf<Any?>()
    } else if (Set::class.java.isAssignableFrom(rawType)) {
        mutableSetOf<Any?>()
    } else {
        TODO()
    }
}

fun toMutableCollection(configObject: ConfigObject, type: Type, list: MutableCollection<Any?>, name: String, provider: ConfigProvider, prefix: String) {
    configObject.asList().forEachIndexed { index, innerObject ->
        if (innerObject.isObject()) {
            val targetType = TargetType(type)
            val superType = targetType.getParameterizedClassArguments().firstOrNull()
            list.add(provider.bind(concatPrefix(prefix, "$name[$index]"), superType as Class<Any>))
        } else if (innerObject.isPrimitive()) {
            val targetType = TargetType(type)
            val rawType = targetType.rawTargetType()
            val superType = targetType.getParameterizedClassArguments().firstOrNull()
            val classType = superType ?: rawType
            list.add(classType.findParser().parse(innerObject, classType, superType?.findParser()))
        }
    }
}

fun KClass<*>.getDefaultMethod(methodName: String): Method? {
    return Class.forName(jvmName + "\$DefaultImpls").methods.filter { it.name == methodName }.firstOrNull()
}

fun KClass<*>.isMethodNullable(method: Method, propertyName: String = ""): Boolean {
    val properties = memberProperties.filter {
        it.name == propertyName || it.name == method.name
    }
    return if (properties.isNotEmpty()) {
        // we have a property
        properties.first().returnType.isMarkedNullable
    } else {
        // this is a method
        method.kotlinFunction?.returnType?.isMarkedNullable ?: false
    }
}

private val METHOD_NAME_REGEX = "^(get|is|has)?(.*)".toRegex()

fun getPropertyName(methodName: String): String {
    return METHOD_NAME_REGEX.replace(methodName) { matchResult ->
        val group = matchResult.groups[2]!!.value
        if (Character.isUpperCase(group[0])) {
            group.decapitalize()
        } else {
            methodName
        }
    }
}

fun concatPrefix(before: String, after: String): String {
    return buildString {
        append(before)
        if (before.isNotEmpty()) {
            append('.')
        }
        append(after)
    }
}