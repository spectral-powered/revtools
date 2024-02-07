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

@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.spectralpowered.revtools.asm

import org.objectweb.asm.tree.ClassNode
import org.spectralpowered.revtools.asm.builder.cfg.InnerClassNormalizer
import org.spectralpowered.revtools.asm.container.Container
import org.spectralpowered.revtools.asm.ir.Class
import org.spectralpowered.revtools.asm.ir.ConcreteClass
import org.spectralpowered.revtools.asm.ir.Modifiers
import org.spectralpowered.revtools.asm.ir.OuterClass
import org.spectralpowered.revtools.asm.ir.value.ValueFactory
import org.spectralpowered.revtools.asm.ir.value.instruction.InstructionFactory
import org.spectralpowered.revtools.asm.type.TypeFactory
import org.spectralpowered.revtools.asm.util.Flags
import java.io.File

data class Package(val components: List<String>, val isConcrete: Boolean) {
    companion object {
        const val SEPARATOR = '/'
        const val SEPARATOR_STR = SEPARATOR.toString()
        const val EXPANSION = '*'
        const val EXPANSION_STR = EXPANSION.toString()
        const val CANONICAL_SEPARATOR = '.'
        const val CANONICAL_SEPARATOR_STR = CANONICAL_SEPARATOR.toString()
        val defaultPackage = Package(EXPANSION_STR)
        val emptyPackage = Package("")
        fun parse(string: String) = Package(
            string.replace(
                CANONICAL_SEPARATOR,
                SEPARATOR
            )
        )
    }

    constructor(name: String) : this(
        name.removeSuffix(EXPANSION_STR)
            .removeSuffix(SEPARATOR_STR)
            .split(SEPARATOR)
            .filter { it.isNotBlank() },
        name.lastOrNull() != EXPANSION
    )

    val concretePackage get() = if (isConcrete) this else Package(concreteName)
    val concreteName get() = components.joinToString(SEPARATOR_STR)
    val canonicalName get() = components.joinToString(CANONICAL_SEPARATOR_STR)
    val fileSystemPath get() = components.joinToString(File.separator)

    val concretized: Package
        get() = when {
            isConcrete -> this
            else -> copy(isConcrete = true)
        }
    val expanded: Package
        get() = when {
            isConcrete -> copy(isConcrete = false)
            else -> this
        }

    fun isParent(other: Package): Boolean = when {
        isConcrete -> this.components == other.components
        this.components.size > other.components.size -> false
        else -> this.components.indices.fold(true) { acc, i ->
            acc && (this[i] == other[i])
        }
    }

    operator fun get(i: Int) = components[i]

    fun isChild(other: Package) = other.isParent(this)
    fun isParent(name: String) = isParent(Package(name))
    fun isChild(name: String) = isChild(Package(name))

    override fun toString() = buildString {
        append(components.joinToString(SEPARATOR_STR))
        if (!isConcrete) {
            if (components.isNotEmpty()) append(SEPARATOR)
            append(EXPANSION)
        }
    }

    override fun hashCode(): Int {
        var result = components.hashCode()
        result = 31 * result + isConcrete.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != this.javaClass) return false
        other as Package
        return this.components == other.components && this.isConcrete == other.isConcrete
    }
}

class ClassGroup private constructor(val config: AsmConfig = AsmConfigBuilder().build()) {

    constructor(
        flags: Flags = Flags.readAll,
        useCachingLoopManager: Boolean = false,
        failOnError: Boolean = true,
        verifyIR: Boolean = false,
        checkClasses: Boolean = false
    ) : this(AsmConfig(flags, useCachingLoopManager, failOnError, verifyIR, checkClasses))

    val value = ValueFactory(this)
    val instruction = InstructionFactory(this)
    val type = TypeFactory(this)
    internal val loopManager: LoopManager by lazy {
        when {
            config.useCachingLoopManager -> CachingLoopManager(this)
            else -> DefaultLoopManager()
        }
    }

    val flags: Flags get() = config.flags
    val failOnError: Boolean get() = config.failOnError
    val verifyIR: Boolean get() = config.verifyIR

    private val _classes = hashMapOf<String, Class>()
    private val outerClasses = hashMapOf<String, Class>()
    private val _ignoredClasses = hashMapOf<String, Class>()
    private val container2class = hashMapOf<Container, MutableSet<Class>>()

    val allClasses: Set<ConcreteClass>
        get() = _classes.values.filterIsInstanceTo(mutableSetOf())

    val ignoredClasses: Set<ConcreteClass>
        get() = allClasses.filter { _ignoredClasses.containsValue(it) }.filterIsInstanceTo(mutableSetOf())

    val classes: Set<ConcreteClass>
        get() = _classes.values.filter { !_ignoredClasses.containsValue(it) }.filterIsInstanceTo(mutableSetOf())

    fun initialize(loader: ClassLoader, vararg containers: Container) {
        initialize(loader, containers.toList())
    }

    fun initialize(vararg containers: Container) {
        initialize(containers.toList())
    }

    fun initialize(loader: ClassLoader, containers: List<Container>) {
        val container2ClassNode = containers.associateWith { it.parse(flags, config.failOnError, loader) }
        initialize(container2ClassNode)
    }

    fun initialize(containers: List<Container>) {
        val container2ClassNode = containers.associateWith { it.parse(flags) }
        initialize(container2ClassNode)
    }

    private fun initialize(container2ClassNode: Map<Container, Map<String, ClassNode>>) {
        for ((container, classNodes) in container2ClassNode) {
            classNodes.forEach { (name, cn) ->
                val klass = ConcreteClass(this, cn)
                _classes[name] = klass
                container2class.getOrPut(container, ::mutableSetOf).add(klass)
            }
        }
        for (klass in _classes.values) {
            InnerClassNormalizer(this).visit(klass)
        }
        _classes.values.forEach { it.init() }
    }

    operator fun get(name: String): Class = _classes[name] ?: outerClasses.getOrPut(name) {
        val pkg = Package.parse(name.substringBeforeLast(Package.SEPARATOR))
        val klassName = name.substringAfterLast(Package.SEPARATOR)
        OuterClass(this, pkg, klassName)
    }

    fun getByPackage(`package`: Package): List<Class> = allClasses.filter { `package`.isParent(it.pkg) }

    fun getSubtypesOf(klass: Class): Set<Class> =
        allClasses.filterTo(mutableSetOf()) { it.isInheritorOf(klass) && it != klass }

    fun getAllSubtypesOf(klass: Class): Set<Class> {
        val result = mutableSetOf(klass)
        var current = getSubtypesOf(klass)
        do {
            val newCurrent = mutableSetOf<Class>()
            for (it in current) {
                result += it
                newCurrent.addAll(getSubtypesOf(it))
            }
            current = newCurrent
        } while (current.isNotEmpty())
        return result
    }

    fun createClass(
        container: Container,
        pkg: Package,
        name: String,
        modifiers: Modifiers = Modifiers(0)
    ): Class {
        val klass = ConcreteClass(this, pkg, name, modifiers)
        _classes[klass.fullName] = klass
        container2class.getOrPut(container, ::mutableSetOf).add(klass)
        return klass
    }

    fun getContainerClasses(container: Container): Set<Class> = container2class.getOrDefault(container, emptySet())

    fun ignoreClass(klass: ConcreteClass): Boolean {
        if (ignoredClasses.contains(klass)) return false
        if (!classes.contains(klass)) return false
        _ignoredClasses[klass.fullName] = klass
        return true
    }
}
