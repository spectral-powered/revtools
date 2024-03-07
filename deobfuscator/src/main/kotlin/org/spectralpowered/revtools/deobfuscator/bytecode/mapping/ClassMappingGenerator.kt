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

package org.spectralpowered.revtools.deobfuscator.bytecode.mapping

import org.objectweb.asm.Opcodes.ACC_NATIVE
import org.objectweb.asm.tree.ClassNode
import org.spectralpowered.revtools.ClassPool
import org.spectralpowered.revtools.deobfuscator.bytecode.mapping.NameGenerator.Companion.isRenamable
import org.spectralpowered.revtools.isAbstract
import org.spectralpowered.revtools.isInterface
import org.spectralpowered.revtools.remap.NameMap
import org.spectralpowered.revtools.superClass

class ClassMappingGenerator(
    private val pool: ClassPool,
    private val nameMap: NameMap
) {

    private val nameGen = NameGenerator()
    private val mapping = mutableMapOf<ClassNode, String>()

    fun generate(): Map<ClassNode, String> {
        for(cls in pool.classes) {
            populateMapping(cls)
        }

        mapping.forEach { (cls, name) ->
            nameMap.mapClass(cls, name)
        }

        return mapping
    }

    private fun populateMapping(cls: ClassNode): String {
        val name = cls.name
        if(mapping.containsKey(cls) || !cls.isRenamable()) {
            return mapping.getOrDefault(cls, name)
        }
        val mappedName = generateName(cls)
        mapping[cls] = mappedName
        return mappedName
    }

    private fun ClassNode.isRenamable(): Boolean {
        if(!name.isRenamable()) return false
        for(method in methods) {
            if(method.access and ACC_NATIVE != 0) {
                return false
            }
        }
        return true
    }

    private fun generateName(cls: ClassNode): String {
        val name = cls.name
        var mappedName = name.packageName

        val superCls = cls.superClass
        if(superCls != null && superCls.name != "java/lang/Object") {
            var superName = populateMapping(superCls)
            superName = superName.className
            mappedName += nameGen.generate(superName + "_Sub")
        } else if(cls.isInterface()) {
            mappedName += nameGen.generate("Interface")
        } else if(cls.isAbstract()) {
            mappedName += nameGen.generate("Class")
        } else {
            mappedName += nameGen.generate("Class")
        }
        return mappedName
    }

    private val String.className: String get() = this.substring(this.lastIndexOf('/') + 1)
    private val String.packageName: String get() = this.substring(0, this.lastIndexOf('/') + 1)
}