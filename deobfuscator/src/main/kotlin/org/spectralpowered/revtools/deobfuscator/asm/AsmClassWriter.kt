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

package org.spectralpowered.revtools.deobfuscator.asm

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Type
import org.spectralpowered.revtools.deobfuscator.asm.tree.ClassGroup
import org.spectralpowered.revtools.deobfuscator.asm.tree.isAssignableFrom
import org.spectralpowered.revtools.deobfuscator.asm.tree.isInterface
import org.spectralpowered.revtools.deobfuscator.asm.tree.superClass
import javax.xml.stream.events.Namespace

class AsmClassWriter(private val group: ClassGroup, flags: Int) : ClassWriter(flags) {

    companion object {
        val OBJECT_INTERNAL_NAME = Type.getInternalName(Any::class.java)
    }

    private val classNames = group.allClasses.associateBy { it.name }

    override fun getCommonSuperClass(type1: String, type2: String): String {
        if(isAssignable(type1, type2)) return type1
        if(isAssignable(type2, type1)) return type2
        var ret = type1
        do {
            ret = checkNotNull(superName(ret))
        } while (!isAssignable(ret, type2))
        return ret
    }

    private fun isAssignable(to: String, from: String): Boolean {
        if(to == from) return true
        val superName = superName(from) ?: return false
        if(isAssignable(to, superName)) return true
        return interfaces(from).any { isAssignable(to, it) }
    }

    private fun interfaces(type: String) = if(type in classNames) {
        classNames.getValue(type).interfaces
    } else {
        Class.forName(type.replace("/", ".")).interfaces.map { Type.getInternalName(it) }
    }

    private fun superName(type: String) = if(type in classNames) {
        classNames.getValue(type).superName
    } else {
        val klass = Class.forName(type.replace("/", "."))
        if(klass.isInterface) {
            OBJECT_INTERNAL_NAME
        } else {
            klass.superclass?.let { Type.getInternalName(it) }
        }
    }
}