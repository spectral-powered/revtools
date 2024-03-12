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

package org.spectralpowered.revtools.deobfuscator.bytecode.transformer

import com.google.common.collect.MultimapBuilder
import org.objectweb.asm.Type
import org.objectweb.asm.Type.*
import org.objectweb.asm.tree.*
import org.spectralpowered.revtools.asm.MemberRef
import org.spectralpowered.revtools.asm.analysis.DataFlowAnalyzer
import org.spectralpowered.revtools.asm.node.*
import org.spectralpowered.revtools.deobfuscator.bytecode.Deobfuscator.Companion.isDeobfuscatedName
import org.spectralpowered.revtools.deobfuscator.bytecode.Transformer
import org.tinylog.kotlin.Logger

class UnusedArgumentTransformer : Transformer() {

    private var count = 0

    private val toRemoveMap = MultimapBuilder.hashKeys().arrayListValues().build<String, MethodNode>()
    private val toRemove = toRemoveMap.asMap()
    private val rootMethods = hashSetOf<String>()

    override fun preTransform(): Boolean {
        for(cls in pool.classes) {
            val parents = cls.parentClasses
            for(method in cls.methods) {
                if(parents.none { it.getMethod(method.memberDesc) != null }) {
                    rootMethods.add(method.memberRef.toString())
                }
            }
        }

        for(cls in pool.classes) {
            for(method in cls.methods) {
                val found = findOverride(method.memberRef, rootMethods) ?: continue
                toRemoveMap.put(found, method)
            }
        }

        val itr = toRemove.iterator()
        for((_, methods) in itr) {
            if(methods.any { !it.hasUnusedArg() }) {
                itr.remove()
            }
        }

        for(method in pool.classes.flatMap { it.methods }) {
            val insns = method.instructions
            for(insn in insns) {
                if(insn !is MethodInsnNode) continue
                val found = findOverride(MemberRef(insn.owner, insn.name, insn.desc), toRemove.keys) ?: continue
                if(insn.previous.intConstant == null) toRemove.remove(found)
            }
        }

        for(method in toRemoveMap.values()) {
            val oldDesc = method.desc
            val newDesc = oldDesc.removeArg()
            method.desc = newDesc
            count++
        }

        for(method in pool.classes.flatMap { it.methods }) {
            val insns = method.instructions
            for(insn in insns) {
                if(insn !is MethodInsnNode) continue
                if(findOverride(MemberRef(insn.owner, insn.name, insn.desc), toRemove.keys) != null) {
                    insn.desc = insn.desc.removeArg()
                    val prevInsn = insn.previous
                    insns.remove(prevInsn)
                }
            }
        }

        return false
    }

    override fun onComplete() {
        Logger.info("Removed $count unused arguments from methods.")
    }

    private fun String.removeArg(): String {
        val type = Type.getMethodType(this)
        return Type.getMethodDescriptor(type.returnType, *type.argumentTypes.copyOf(type.argumentTypes.size - 1))
    }

    private val MethodNode.lastArgIndex: Int get() {
        val offset = if(isStatic()) 1 else 0
        return (Type.getArgumentsAndReturnSizes(desc) shr 2) - offset - 1
    }

    private fun MethodNode.hasUnusedArg(): Boolean {
        val argTypes = Type.getArgumentTypes(desc)
        if(argTypes.isEmpty()) return false
        val lastArg = argTypes.last()
        if(lastArg !in listOf(BYTE_TYPE, SHORT_TYPE, INT_TYPE)) return false
        if(isAbstract()) return true
        for(insn in instructions) {
            if(insn !is VarInsnNode) continue
            if(insn.`var` == lastArgIndex) return false
        }
        return name.isDeobfuscatedName()
    }

    private val ClassNode.superClasses: Set<ClassNode> get() = parentClasses.flatMap { it.superClasses.plus(it) }.toSet()

    private fun findOverride(memberRef: MemberRef, methods: Set<String>): String? {
        val method = memberRef.toString()
        if(method in methods) return method
        if(memberRef.name == "<init>") return null
        val cls = pool.findClass(memberRef.owner) ?: return null
        for(superCls in cls.superClasses) {
            return findOverride(MemberRef(superCls.name, memberRef.name, memberRef.desc), methods) ?: continue
        }
        return null
    }
}