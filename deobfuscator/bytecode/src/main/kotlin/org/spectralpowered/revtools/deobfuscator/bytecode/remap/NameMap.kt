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

package org.spectralpowered.revtools.deobfuscator.bytecode.remap

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.spectralpowered.revtools.asm.ClassPool
import org.spectralpowered.revtools.asm.MemberRef
import org.spectralpowered.revtools.asm.node.memberRef
import org.spectralpowered.revtools.asm.remap.AsmRemapper
import org.spectralpowered.revtools.asm.util.DisjointSet

class NameMap(pool: ClassPool) : AsmRemapper() {

    private val inheritedMethodSets = pool.createInheritedMethodSets()
    private val inheritedFieldSets = pool.createInheritedFieldSets()

    val classMappings = mutableMapOf<String, String>()
    val methodMappings = mutableMapOf<DisjointSet.Partition<MemberRef>, String>()
    val fieldMappings = mutableMapOf<DisjointSet.Partition<MemberRef>, String>()

    fun renameClass(cls: ClassNode, name: String) {
        classMappings.putIfAbsent(cls.name, name)
    }

    fun renameMethod(method: MethodNode, name: String) {
        val member = inheritedMethodSets[method.memberRef]!!
        methodMappings.putIfAbsent(member, name)
    }

    fun renameField(field: FieldNode, name: String) {
        val member = inheritedFieldSets[field.memberRef]!!
        fieldMappings.putIfAbsent(member, name)
    }

    fun hasMethodMapping(method: MethodNode) = methodMappings.containsKey(inheritedMethodSets[method.memberRef])
    fun hasFieldMapping(field: FieldNode) = fieldMappings.containsKey(inheritedFieldSets[field.memberRef])

    override fun map(internalName: String): String {
        return classMappings.getOrDefault(internalName, internalName)
    }

    override fun mapMethodName(owner: String, name: String, descriptor: String): String {
        val member = MemberRef(owner, name, descriptor)
        val partition = inheritedMethodSets[member] ?: return name
        return methodMappings.getOrDefault(partition, name)
    }

    override fun mapFieldName(owner: String, name: String, descriptor: String): String {
        val member = MemberRef(owner, name, descriptor)
        val partition = inheritedFieldSets[member] ?: return name
        return fieldMappings.getOrDefault(partition, name)
    }
}