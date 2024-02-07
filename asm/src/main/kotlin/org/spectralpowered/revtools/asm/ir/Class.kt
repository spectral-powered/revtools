/*
 * Copyright (C) 2024 Spectral Powered <https://github.com/spectral-powered>
 * @author Kyle Escobar <https://github.com/kyle-escobar>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

@file:Suppress("unused")

package org.spectralpowered.revtools.asm.ir

import org.objectweb.asm.tree.ClassNode
import org.spectralpowered.revtools.asm.ClassGroup
import org.spectralpowered.revtools.asm.InvalidStateException
import org.spectralpowered.revtools.asm.Package
import org.spectralpowered.revtools.asm.UnknownInstanceException
import org.spectralpowered.revtools.asm.helper.assert.ktassert
import org.spectralpowered.revtools.asm.type.ClassType
import org.spectralpowered.revtools.asm.type.SystemTypeNames
import org.spectralpowered.revtools.asm.type.Type
import org.spectralpowered.revtools.asm.type.TypeFactory

@Suppress("MemberVisibilityCanBePrivate")
abstract class Class : Node {
    protected data class MethodKey(val name: String, val desc: MethodDescriptor) {
        constructor(tf: TypeFactory, name: String, desc: String) : this(name, MethodDescriptor.fromDesc(tf, desc))

        override fun toString() = "$name$desc"
    }

    protected infix fun String.to(desc: MethodDescriptor) = MethodKey(this, desc)

    protected data class FieldKey(val name: String, val type: Type)

    protected infix fun String.to(type: Type) = FieldKey(this, type)

    internal val cn: ClassNode
    val pkg: Package
    protected val innerMethods = mutableMapOf<MethodKey, Method>()
    protected val innerFields = mutableMapOf<FieldKey, Field>()
    protected var superClassName: String? = null
    protected val interfaceNames = mutableSetOf<String>()
    protected var outerClassName: String? = null
    protected var outerMethodName: String? = null
    protected var outerMethodDesc: String? = null
    protected var innerClassesMap = mutableMapOf<String, Modifiers>()

    val allMethods get() = innerMethods.values.toSet()
    val constructors: Set<Method> get() = allMethods.filterTo(mutableSetOf()) { it.isConstructor }
    val methods: Set<Method> get() = allMethods.filterNotTo(mutableSetOf()) { it.isConstructor }
    val fields get() = innerFields.values.toSet()

    val fullName: String

    val canonicalDesc
        get() = fullName.replace(Package.SEPARATOR, Package.CANONICAL_SEPARATOR)

    var superClass
        get() = superClassName?.let { group[it] }
        set(value) {
            superClassName = value?.fullName
        }

    val interfaces: Set<Class> get() = interfaceNames.mapTo(mutableSetOf()) { group[it] }

    var outerClass
        get() = outerClassName?.let { group[it] }
        set(value) {
            outerClassName = value?.fullName
        }

    var outerMethod
        get() = outerMethodName?.let { name ->
            outerMethodDesc?.let { desc ->
                outerClass?.getMethod(name, desc)
            }
        }
        set(value) {
            outerMethodName = value?.name
            outerMethodDesc = value?.desc?.asmDesc
            outerClass = value?.klass
        }

    val innerClasses get() = innerClassesMap.map { group[it.key] to it.value }.toMap()

    override val asmDesc
        get() = "L$fullName;"

    constructor(
        group: ClassGroup,
        pkg: Package,
        name: String,
        modifiers: Modifiers = Modifiers(0)
    ) : super(group, name, modifiers) {
        ktassert(pkg.isConcrete)
        this.pkg = pkg
        this.fullName = if (pkg == Package.emptyPackage) name else "$pkg${Package.SEPARATOR}$name"
        this.cn = ClassNode()
        this.cn.name = fullName
        this.cn.access = modifiers.value
    }

    constructor(
        group: ClassGroup,
        cn: ClassNode
    ) : super(group, cn.name.substringAfterLast(Package.SEPARATOR), Modifiers(cn.access)) {
        this.cn = cn
        this.pkg = Package.parse(
            cn.name.substringBeforeLast(Package.SEPARATOR, "")
        )
        this.fullName = if (pkg == Package.emptyPackage) name else "$pkg${Package.SEPARATOR}$name"
        this.superClassName = cn.superName
        this.interfaceNames.addAll(cn.interfaces.toMutableSet())
        this.outerClassName = cn.outerClass
        this.outerMethodName = cn.outerMethod
        this.outerMethodDesc = cn.outerMethodDesc
    }

    internal fun init() {
        this.innerClassesMap.putAll(cn.innerClasses.map { it.name to Modifiers(it.access) }.toMutableSet())
        this.outerClassName = cn.outerClass
        for (fieldNode in cn.fields) {
            val field = Field(group, this, fieldNode)
            innerFields[field.name to field.type] = field
        }
        cn.methods.forEach {
            val desc = MethodDescriptor.fromDesc(group.type, it.desc)
            innerMethods[it.name to desc] = Method(group, this, it)
        }
        cn.methods = this.allMethods.map { it.mn }
    }

    val allAncestors get() = listOfNotNull(superClass) + interfaces

    val asType: ClassType by lazy { ClassType(this) }

    abstract fun isAncestorOf(other: Class, outerClassBehavior: Boolean = true): Boolean
    fun isInheritorOf(other: Class, outerClassBehavior: Boolean = true) = other.isAncestorOf(this, outerClassBehavior)

    abstract fun getFieldConcrete(name: String, type: Type): Field?
    abstract fun getMethodConcrete(name: String, desc: MethodDescriptor): Method?

    fun getFields(name: String): Set<Field> = fields.filterTo(mutableSetOf()) { it.name == name }
    abstract fun getField(name: String, type: Type): Field

    fun getMethods(name: String): Set<Method> = methods.filterTo(mutableSetOf()) { it.name == name }
    fun getMethod(name: String, desc: String) = getMethod(name, MethodDescriptor.fromDesc(group.type, desc))
    fun getMethod(name: String, returnType: Type, vararg argTypes: Type) =
        this.getMethod(name, MethodDescriptor(argTypes.toList(), returnType))

    abstract fun getMethod(name: String, desc: MethodDescriptor): Method

    /**
     * creates a new field with given name and type and adds is to this klass
     * @throws InvalidStateException if there already exists field with given parameters
     */
    fun addField(name: String, type: Type): Field {
        if ((name to type) in innerFields) throw InvalidStateException("Field $name: $type already exists in $this")
        val field = Field(group, this, name, type)
        innerFields[name to type] = field
        return field
    }

    /**
     * creates a new method with given name and descriptor and adds is to this klass
     * @throws InvalidStateException if there already exists method with given parameters
     */
    fun addMethod(name: String, desc: MethodDescriptor): Method {
        if ((name to desc) in innerMethods) throw InvalidStateException("Method $name: $desc already exists in $this")
        val method = Method(group, this, name, desc)
        innerMethods[name to desc] = method
        return method
    }

    fun addMethod(name: String, returnType: Type, vararg argTypes: Type) =
        addMethod(name, MethodDescriptor(argTypes.toList(), returnType))

    internal fun updateMethod(old: MethodDescriptor, new: MethodDescriptor, method: Method) {
        innerMethods.remove(method.name to old)
        innerMethods[method.name to new] = method

    }

    fun removeField(field: Field) = innerFields.remove(field.name to field.type)
    fun removeMethod(method: Method) = innerMethods.remove(method.name to method.desc)

    fun addInnerClass(klass: Class, modifiers: Modifiers) {
        innerClassesMap[klass.fullName] = modifiers
    }

    override fun toString() = fullName
    override fun hashCode() = fullName.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != this.javaClass) return false
        other as Class
        return this.name == other.name && this.pkg == other.pkg
    }
}

class ConcreteClass : Class {
    constructor(group: ClassGroup, cn: ClassNode) : super(group, cn)
    constructor(
        group: ClassGroup,
        pkg: Package,
        name: String,
        modifiers: Modifiers = Modifiers(0)
    ) : super(group, pkg, name, modifiers)

    override fun getFieldConcrete(name: String, type: Type): Field? =
        innerFields.getOrElse(name to type) { superClass?.getFieldConcrete(name, type) }

    override fun getMethodConcrete(name: String, desc: MethodDescriptor): Method? =
        innerMethods.getOrElse(name to desc) {
            val concreteMethod = allAncestors.mapNotNull { it as? ConcreteClass }
                .map { it.getMethodConcrete(name, desc) }
                .firstOrNull()
            val res: Method? = concreteMethod
                ?: allAncestors.firstNotNullOfOrNull { it as? OuterClass }
                    ?.getMethodConcrete(name, desc)
            res
        }

    override fun getField(name: String, type: Type) = innerFields.getOrElse(name to type) {
        var parents = allAncestors.toList()

        var result: Field?
        do {
            result =
                parents.mapNotNull { it as? ConcreteClass }.firstNotNullOfOrNull { it.getFieldConcrete(name, type) }
            parents = parents.flatMap { it.allAncestors }
        } while (result == null && parents.isNotEmpty())

        result
            ?: allAncestors.mapNotNull { it as? OuterClass }.map { it.getFieldConcrete(name, type) }.firstOrNull()
            ?: throw UnknownInstanceException("No field \"$name\" in class $this")
    }

    override fun getMethod(name: String, desc: MethodDescriptor): Method {
        val methodDesc = name to desc
        return innerMethods.getOrElse(methodDesc) {
            var parents = allAncestors.toList()

            var result: Method?
            do {
                result = parents.mapNotNull { it as? ConcreteClass }
                    .firstNotNullOfOrNull { it.getMethodConcrete(name, desc) }
                parents = parents.flatMap { it.allAncestors }
            } while (result == null && parents.isNotEmpty())

            result
                ?: allAncestors.mapNotNull { it as? OuterClass }.map { it.getMethodConcrete(name, desc) }.firstOrNull()
                ?: throw UnknownInstanceException("No method \"$methodDesc\" in $this")
        }
    }

    override fun isAncestorOf(other: Class, outerClassBehavior: Boolean): Boolean {
        if (this == other) return true
        else {
            other.superClass?.let {
                if (isAncestorOf(it)) return true
            }
            for (it in other.interfaces) if (isAncestorOf(it, outerClassBehavior)) return true
        }
        return false
    }
}

class OuterClass(
    group: ClassGroup,
    pkg: Package,
    name: String,
    modifiers: Modifiers = Modifiers(0)
) : Class(group, pkg, name, modifiers) {
    override fun getFieldConcrete(name: String, type: Type) = getField(name, type)
    override fun getMethodConcrete(name: String, desc: MethodDescriptor) = getMethod(name, desc)

    override fun getField(name: String, type: Type): Field = innerFields.getOrPut(name to type) {
        addField(name, type)
    }

    override fun getMethod(name: String, desc: MethodDescriptor): Method {
        return innerMethods.getOrPut(name to desc) {
            addMethod(name, desc)
        }
    }

    override fun isAncestorOf(other: Class, outerClassBehavior: Boolean) = when (this.fullName) {
        other.fullName -> true
        SystemTypeNames.objectClass -> true
        else -> outerClassBehavior
    }
}
