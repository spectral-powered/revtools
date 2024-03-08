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

package org.spectralpowered.revtools.node

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.spectralpowered.revtools.util.*
import org.spectralpowered.revtools.util.isAbstract
import org.spectralpowered.revtools.util.isPrivate
import org.spectralpowered.revtools.util.isPublic
import java.io.InputStream

/**
 * Extension property for ClassNode to hold a reference to the ClassPool.
 * Also initializes the cls property for each method and field in the ClassNode.
 */
var ClassNode.pool: ClassPool by fieldWithSetter {
    methods.forEach { it.cls = this }
    fields.forEach { it.cls = this }
}

/**
 * Extension property for ClassNode to hold the ClassType.
 * Default value is ClassType.RESOLVED.
 */
var ClassNode.classType: ClassType by field { ClassType.RESOLVED }

/**
 * Extension property for ClassNode to hold the jar index.
 * Default value is -1.
 */
var ClassNode.jarIndex: Int by field { -1 }

/**
 * Extension property for ClassNode to hold a reference to the superclass.
 * Default value is null.
 */
var ClassNode.superClass: ClassNode? by nullField()
val ClassNode.interfaceClasses: MutableSet<ClassNode> by mutableSetField()
val ClassNode.implementerClasses: MutableList<ClassNode> by mutableListField()
val ClassNode.subClasses: MutableList<ClassNode> by mutableListField()

val ClassNode.parentClasses get() = listOfNotNull(superClass).plus(interfaceClasses)
val ClassNode.childClasses get() = subClasses.plus(implementerClasses)

val ClassNode.allChildClasses: Set<ClassNode> get() = childClasses.flatMap { it.allChildClasses.plus(it) }.toSet()
val ClassNode.allParentClasses: Set<ClassNode> get() = parentClasses.flatMap { it.allParentClasses.plus(it) }.toSet()

val ClassNode.hierarchyClasses: Set<ClassNode> get() = allChildClasses.flatMap { it.allParentClasses.plus(it).distinct() }.toSet()

val ClassNode.key get() = name

fun ClassNode.isPublic() = access.isPublic()
fun ClassNode.isPrivate() = access.isPrivate()
fun ClassNode.isAbstract() = access.isAbstract()
fun ClassNode.isInterface() = access.isInterface()

/**
 * Extension function to get a method by its name and descriptor.
 */
fun ClassNode.getMethod(name: String, desc: String) = methods.firstOrNull { it.name == name && it.desc == desc }

/**
 * Extension function to get a field by its name and descriptor.
 */
fun ClassNode.getField(name: String, desc: String) = fields.firstOrNull { it.name == name && it.desc == desc }

/**
 * Extension function to find a method in the class or its superclass by its name and descriptor.
 */
fun ClassNode.findMethod(name: String, desc: String): MethodNode? {
    val ret = getMethod(name, desc)
    if(ret != null) return ret
    return superClass?.findMethod(name, desc)
}

/**
 * Extension function to find a field in the class or its superclass by its name and descriptor.
 */
fun ClassNode.findField(name: String, desc: String): FieldNode? {
    val ret = getField(name, desc)
    if(ret != null) return ret
    return superClass?.findField(name, desc)
}

/**
 * Extension function to get all virtual methods for a given method.
 */
fun ClassNode.findInheritedMethods(name: String, desc: String): List<MethodNode> {
    fun findParentMethods(cls: ClassNode?, name: String, desc: String, methods: MutableList<MethodNode>): MutableList<MethodNode> {
        if(cls == null) return methods
        val method = cls.getMethod(name, desc)
        if(method != null && !method.isStatic()) {
            methods.add(method)
        }
        val baseMethods = findParentMethods(cls.superClass, name, desc, mutableListOf())
        for(itf in cls.interfaceClasses) {
            baseMethods.addAll(findParentMethods(itf, name, desc, mutableListOf()))
        }
        return if(baseMethods.isEmpty()) methods else baseMethods
    }

    fun findChildMethods(cls: ClassNode?, name: String, desc: String, methods: MutableList<MethodNode>, visited: HashSet<ClassNode>) {
        if(cls == null || visited.contains(cls)) return
        visited.add(cls)
        val method = cls.getMethod(name, desc)
        if(method != null && !method.isStatic()) {
            methods.add(method)
        }
        for(child in cls.childClasses) {
            findChildMethods(child, name, desc, methods, visited)
        }
    }

    val method = findMethod(name, desc) ?: return emptyList()
    val results = mutableListOf<MethodNode>()
    if(method.isStatic()) {
        results.add(method)
        return results
    }
    val parents = findParentMethods(method.cls, method.name, method.desc, mutableListOf())
    for(parent in parents) {
        findChildMethods(parent.cls, parent.name, parent.desc, results, hashSetOf())
    }
    return results
}

/**
 * Extension function to create a ClassNode from a byte array.
 */
fun ClassNode.fromBytes(bytes: ByteArray, flags: Int = ClassReader.SKIP_FRAMES): ClassNode {
    val reader = ClassReader(bytes)
    reader.accept(this, flags)
    return this
}

/**
 * Extension function to convert a ClassNode to a byte array.
 */
fun ClassNode.toBytes(flags: Int = ClassWriter.COMPUTE_MAXS): ByteArray {
    val writer = ClassWriter(flags)
    this.accept(writer)
    return writer.toByteArray()
}

/**
 * Extension function to create a ClassNode from an InputStream.
 */
fun ClassNode.fromInputStream(input: InputStream, flags: Int = ClassReader.SKIP_FRAMES) = this.fromBytes(input.readAllBytes(), flags)

/**
 * Extension function to reset the ClassNode.
 */
fun ClassNode.reset() {
    superClass = null
    interfaceClasses.clear()
    subClasses.clear()
    implementerClasses.clear()
}

/**
 * Extension function to initialize the ClassNode.
 */
fun ClassNode.init() {
    superClass = pool.findClass(superName)
    superClass?.subClasses?.add(this)
    for(itf in interfaces.mapNotNull { pool.findClass(it) }) {
        interfaceClasses.add(itf)
        itf.implementerClasses.add(this)
    }
}