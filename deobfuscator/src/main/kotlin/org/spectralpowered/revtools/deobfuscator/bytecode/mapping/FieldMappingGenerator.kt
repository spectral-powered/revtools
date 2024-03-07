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

import org.objectweb.asm.Type
import org.objectweb.asm.tree.FieldNode
import org.spectralpowered.revtools.*
import org.spectralpowered.revtools.deobfuscator.bytecode.mapping.NameGenerator.Companion.isRenamable
import org.spectralpowered.revtools.remap.NameMap

class FieldMappingGenerator(
    private val pool: ClassPool,
    private val nameMap: NameMap
) {
    private val nameGen = NameGenerator()
    private val mapping = hashMapOf<FieldNode, String>()

    fun generate(): Map<FieldNode, String> {
        for(field in pool.classes.flatMap { it.fields }) {
            if(!field.name.isRenamable()) continue
            var name = ""
            if(field.isPrivate()) name += "Private"
            if(field.isStatic()) name += "Static"
            if(field.isFinal()) name += "Final"
            name += "Field"
            val type = Type.getType(field.desc)
            mapping[field] = nameMap[field.key] ?: generateName(type, name)
        }

        mapping.forEach { (field, name) ->
            nameMap.mapField(field, name)
        }

        return mapping
    }

    private fun generateName(type: Type, postfix: String): String {
        val dims: String
        val elementType: Type
        if(type.sort == Type.ARRAY) {
            dims = "Array".repeat(type.dimensions)
            elementType = type.elementType
        } else {
            dims = ""
            elementType = type
        }
        val prefix = when(elementType.sort) {
            in listOf(Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT, Type.LONG, Type.FLOAT, Type.DOUBLE) -> {
                elementType.className + dims
            }
            Type.OBJECT -> {
                val className = nameMap[elementType.internalName] ?: ""
                className.substring(className.lastIndexOf('/') + 1) + dims
            }
            else -> throw IllegalArgumentException("Unknown field type $elementType")
        }
        return nameGen.generate((postfix).replaceFirstChar { it.lowercaseChar() })
    }
}