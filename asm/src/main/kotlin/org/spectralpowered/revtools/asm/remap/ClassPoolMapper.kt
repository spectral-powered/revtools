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

package org.spectralpowered.revtools.asm.remap

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode
import org.spectralpowered.revtools.asm.ClassPool
import org.spectralpowered.revtools.asm.MemberRef
import org.spectralpowered.revtools.asm.node.isInitializer
import org.spectralpowered.revtools.asm.node.nextReal
import kotlin.math.max

class ClassPoolMapper(
    private val pool: ClassPool,
    private val remapper: AsmRemapper,
) {
    private class Initializer(val instructions: InsnList, val maxStack: Int) {
        val references = instructions.asSequence()
            .filterIsInstance<FieldInsnNode>()
            .filter { it.opcode == GETSTATIC }
            .map(::MemberRef)
            .toSet()
    }
    private class Field(val owner: String, val node: FieldNode, val initializer: Initializer?)
    private class Method(val owner: String, val node: MethodNode)

    private val methods = mutableListOf<Method>()
    private val fields = mutableMapOf<MemberRef, Field>()
    private val splicedFields = mutableSetOf<MemberRef>()

    fun remap() {
        extractFields()
        extractMethods()

        for(cls in pool.classes) {
            cls.remap(remapper)
        }

        spliceFields()
        spliceMethods()
        removeEmtpyClinitMethods()
    }

    private fun extractMethods() {
        for(cls in pool.classes) {
            cls.methods.removeIf { method ->
                val oldOwner = remapper.mapType(cls.name)
                val newOwner = remapper.mapMethodOwner(cls.name, method.name, method.desc)

                if(oldOwner == newOwner) {
                    return@removeIf false
                }

                method.remap(remapper, cls.name)
                methods += Method(newOwner, method)
                return@removeIf true
            }
        }
    }

    private fun extractFields() {
        for(cls in pool.classes) {
            cls.fields.removeIf { field ->
                val oldOwner = remapper.mapType(cls.name)
                val newOwner = remapper.mapFieldOwner(cls.name, field.name, field.desc)

                if(oldOwner == newOwner) {
                    return@removeIf false
                }

                val initializer = extractInitializer(cls, field)
                field.remap(remapper, cls.name)

                val newMember = MemberRef(newOwner, field.name, field.desc)
                fields[newMember] = Field(newOwner, field, initializer)
                return@removeIf true
            }
        }
    }

    private fun extractInitializer(cls: ClassNode, field: FieldNode): Initializer? {
        val clinit = cls.methods.find { it.isInitializer() } ?: return null
        val initializer = remapper.getFieldInitializer(cls.name, field.name, field.desc) ?: return null
        val insns = InsnList()
        for(insn in initializer) {
            clinit.instructions.remove(insn)
            insns.add(insn)
            insn.remap(remapper)
        }
        return Initializer(insns, clinit.maxStack)
    }

    private fun spliceMethods() {
        for(method in methods) {
            val cls = pool.getClass(method.owner) ?: error("Class ${method.owner} does not exist in the pool.")
            cls.version = V1_8
            cls.methods.add(method.node)
        }
    }

    private fun spliceFields() {
        for(member in fields.keys) {
            spliceField(member)
        }
    }

    private fun spliceField(member: MemberRef) {
        if(!splicedFields.add(member)) {
            return
        }

        val field = fields[member] ?: return
        val cls = pool.getClass(field.owner) ?: error("Class ${field.owner} does not exist in the pool.")

        if(field.initializer != null) {
            for(fieldRef in field.initializer.references) {
                spliceField(fieldRef)
            }

            val clinit = cls.methods.find { it.isInitializer() } ?: createClinitMethod()
            check(clinit.instructions.hasReturnExit()) {
                "Class ${cls.name} <clinit> does not have RETURN exit."
            }

            clinit.maxStack = max(clinit.maxStack, field.initializer.maxStack)
            clinit.instructions.insertBefore(clinit.instructions.last, field.initializer.instructions)
        }

        cls.version = V1_8
        cls.fields.add(field.node)
    }

    private fun InsnList.hasReturnExit(): Boolean {
        val insn = singleOrNull { it.opcode == RETURN }
        return insn != null && insn.nextReal == null
    }

    private fun removeEmtpyClinitMethods() {
        for(cls in pool.classes) {
            val clinit = cls.methods.find { it.isInitializer() } ?: continue
            val first = clinit.instructions.firstOrNull { it.opcode != -1 }
            if(first != null && first.opcode == RETURN) {
                cls.methods.remove(clinit)
            }
        }
    }

    private fun createClinitMethod(): MethodNode {
        error("")
    }
}