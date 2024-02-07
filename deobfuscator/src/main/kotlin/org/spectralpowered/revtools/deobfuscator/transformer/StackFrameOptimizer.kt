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

package org.spectralpowered.revtools.deobfuscator.transformer

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.CheckClassAdapter
import org.spectralpowered.revtools.deobfuscator.Logger
import org.spectralpowered.revtools.deobfuscator.Transformer
import org.spectralpowered.revtools.deobfuscator.asm.tree.ClassGroup
import org.spectralpowered.revtools.deobfuscator.asm.tree.isIgnored

class StackFrameOptimizer : Transformer {

    override fun run(group: ClassGroup) {
        val newNodes = mutableListOf<Pair<ClassNode, ClassNode>>()
        group.classes.forEach { cls ->
            val newNode = ClassNode()
            val writer = Writer(group)
            cls.accept(writer)

            checkDataFlow(cls.name, writer.toByteArray())

            val reader = ClassReader(writer.toByteArray())
            reader.accept(newNode, ClassReader.SKIP_FRAMES)

            newNodes.add(cls to newNode)
        }

        newNodes.forEach { (old, new) ->
            group.replaceClass(old, new)
        }

        group.build()

        Logger.info("Fixed method stack frames for ${group.allClasses.size} classes.")
    }

    private fun checkDataFlow(className: String, data: ByteArray) {
        try {
            val reader = ClassReader(data)
            val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
            val checker = CheckClassAdapter(writer, true)
            reader.accept(checker, 0)
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    private class Writer(group: ClassGroup) : ClassWriter(COMPUTE_FRAMES or COMPUTE_MAXS) {

        companion object {
            val OBJECT_INTERNAL_NAME: String = Type.getInternalName(Any::class.java)
        }

        private val classNames = group.allClasses.associateBy { it.name }

        override fun getCommonSuperClass(type1: String, type2: String): String {
            if(isAssignable(type1, type2)) return type2
            if(isAssignable(type2, type1)) return type1
            var t1 = type1
            do {
                t1 = checkNotNull(superClassName(t1, classNames))
            } while(!isAssignable(type2, t1))
            return t1
        }

        private fun isAssignable(from: String, to: String): Boolean {
            if(from == to) return true
            val superClass = superClassName(from, classNames) ?: return false
            if(isAssignable(superClass, to)) return true
            return interfaceNames(from).any { isAssignable(it, to) }
        }

        private fun interfaceNames(type: String): List<String> {
            return if(type in classNames) {
                classNames.getValue(type).interfaces
            } else {
                Class.forName(type.replace('/', '.')).interfaces.map { Type.getInternalName(it) }
            }
        }

        private fun superClassName(type: String, classNames: Map<String, ClassNode>): String? {
            return if(type in classNames) {
                classNames.getValue(type).superName
            } else {
                val c = Class.forName(type.replace('/', '.'))
                if(c.isInterface) {
                    OBJECT_INTERNAL_NAME
                } else {
                    c.superclass?.let { Type.getInternalName(it) }
                }
            }
        }
    }
}