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

@file:Suppress("UnusedUnaryOperator")

package org.spectralpowered.revtools.tree

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

var ClassNode.pool: ClassPool by fieldWithSetter {
    methods.forEach { it.cls = this }
    fields.forEach { it.cls = this }
}

var ClassNode.classType: ClassType by field { ClassType.RESOLVED }
var ClassNode.jarIndex: Int by field { -1 }

var ClassNode.superClass: ClassNode? by nullField()
val ClassNode.interfaceClasses: MutableSet<ClassNode> by mutableSetField()
val ClassNode.implementerClasses: MutableList<ClassNode> by mutableListField()
val ClassNode.subClasses: MutableList<ClassNode> by mutableListField()
val ClassNode.parentClasses get() = listOfNotNull(superClass).plus(interfaceClasses)
val ClassNode.childClasses get() = subClasses.plus(implementerClasses)

val ClassNode.key get() = name

fun ClassNode.isPublic() = access.isPublic()
fun ClassNode.isPrivate() = access.isPrivate()
fun ClassNode.isAbstract() = access.isAbstract()
fun ClassNode.isInterface() = access.isInterface()

fun ClassNode.getMethod(name: String, desc: String) = methods.firstOrNull { it.name == name && it.desc == desc }
fun ClassNode.getField(name: String, desc: String) = fields.firstOrNull { it.name == name && it.desc == desc }

fun ClassNode.findMethod(name: String, desc: String): MethodNode? {
    val ret = getMethod(name, desc)
    if(ret != null) return null
    return superClass?.findMethod(name, desc)
}

fun ClassNode.findField(name: String, desc: String): FieldNode? {
    val ret = getField(name, desc)
    if(ret != null) return ret
    return superClass?.findField(name, desc)
}

fun ClassNode.getVirtualMethods(method: MethodNode): List<MethodNode> {
    if (method.isStatic()) {
        return listOf(method)
    }

    val virtualMethods = mutableListOf<MethodNode>()
    val visited = mutableSetOf<ClassNode>()
    val toVisit = ArrayDeque<ClassNode>()

    method.cls.let { toVisit.add(it) }

    while (toVisit.isNotEmpty()) {
        val currentClassFile = toVisit.removeFirstOrNull() ?: continue
        if (visited.contains(currentClassFile)) {
            continue
        }
        visited.add(currentClassFile)
        currentClassFile.findMethod(method.name, method.desc)?.let { foundMethod ->
            if (!foundMethod.isStatic()) {
                virtualMethods.add(foundMethod)
            }
        }
        currentClassFile.superClass?.takeIf { !visited.contains(it) }?.also { toVisit.add(it) }
        currentClassFile.interfaceClasses
            .filterNot { visited.contains(it) }
            .forEach { toVisit.add(it) }
    }

    return virtualMethods.distinct()
}


fun ClassNode.fromBytes(bytes: ByteArray, flags: Int = ClassReader.SKIP_FRAMES): ClassNode {
    val reader = ClassReader(bytes)
    reader.accept(this, flags)
    return this
}

fun ClassNode.toBytes(flags: Int = ClassWriter.COMPUTE_MAXS): ByteArray {
    val writer = ClassWriter(flags)
    this.accept(writer)
    return writer.toByteArray()
}

fun ClassNode.fromInputStream(input: InputStream, flags: Int = ClassReader.SKIP_FRAMES) = this.fromBytes(input.readAllBytes(), flags)

fun ClassNode.reset() {
    superClass = null
    interfaceClasses.clear()
    subClasses.clear()
    implementerClasses.clear()
}

fun ClassNode.init() {
    superClass = pool.findClass(superName)
    superClass?.subClasses?.add(this)
    for(itf in interfaces.mapNotNull { pool.findClass(it) }) {
        interfaceClasses.add(itf)
        itf.implementerClasses.add(this)
    }
}