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

package org.spectralpowered.revtools.asm.node

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.ACC_INTERFACE
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.CheckClassAdapter
import org.spectralpowered.revtools.asm.ClassPool
import org.spectralpowered.revtools.asm.MemberDesc
import org.spectralpowered.revtools.asm.util.*

fun ClassNode.init(pool: ClassPool) {
    this.pool = pool
    methods.forEach { it.init(this) }
    fields.forEach { it.init(this) }
}

fun ClassNode.build() {
    superClass = superName?.let { pool.findClass(it) }
    interfaceClasses = interfaces.mapNotNull { pool.findClass(it) }.toMutableSet()
}

var ClassNode.pool: ClassPool by field()
var ClassNode.isIgnored: Boolean by field { false }
var ClassNode.isRuntime: Boolean by field { false }
var ClassNode.jarIndex: Int by field { -1 }

var ClassNode.superClass: ClassNode? by nullField()
var ClassNode.interfaceClasses: MutableSet<ClassNode> by mutableSetField()

val ClassNode.memberMethods get() = methods.map { it.memberDesc }.toList()
val ClassNode.memberFields get() = fields.map { it.memberDesc }.toList()

fun ClassNode.isInterface() = (access and ACC_INTERFACE) != 0

fun ClassNode.getMethod(name: String, desc: String) = methods.firstOrNull { it.name == name && it.desc == desc }
fun ClassNode.getField(name: String, desc: String) = fields.firstOrNull { it.name == name && it.desc == desc }
fun ClassNode.getMethod(memberDesc: MemberDesc) = getMethod(memberDesc.name, memberDesc.desc)
fun ClassNode.getField(memberDesc: MemberDesc) = getField(memberDesc.name, memberDesc.desc)

fun ClassNode.isAssignableFrom(other: ClassNode): Boolean {
    return this == other || isSuperClassOf(other) || isInterfaceOf(other)
}

fun ClassNode.isSuperClassOf(other: ClassNode): Boolean {
    val superCls = other.superClass ?: return false
    if(superCls == this) return true
    return isSuperClassOf(superCls)
}

fun ClassNode.isInterfaceOf(other: ClassNode): Boolean {
    for(superInterface in other.interfaceClasses) {
        if(superInterface == this || isSuperClassOf(superInterface)) return true
    }
    return false
}

fun ClassNode.fromBytes(bytes: ByteArray, flags: Int): ClassNode {
    val reader = ClassReader(bytes)
    reader.accept(JsrInliner(this), flags)
    return this
}

fun ClassNode.toBytes(): ByteArray {
    val writer = AsmClassWriter(pool)
    accept(writer)
    accept(CheckClassAdapter(NopClassVisitor, true))
    return writer.toByteArray()
}

fun ClassNode.getMethodAccess(memberDesc: MemberDesc): Int? = getMethod(memberDesc)?.access
fun ClassNode.getFieldAccess(memberDesc: MemberDesc): Int? = getField(memberDesc)?.access