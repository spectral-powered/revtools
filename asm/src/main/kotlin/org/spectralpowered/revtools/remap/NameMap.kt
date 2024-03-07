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

package org.spectralpowered.revtools.remap

import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.spectralpowered.revtools.*

class NameMap : Remapper() {

    private val mappings = hashMapOf<String, String>()

    fun mapClass(cls: ClassNode, name: String) { mappings[cls.key] = name }

    fun mapMethod(method: MethodNode, name: String) {
        if(mappings.containsKey(method.key)) return
        mappings[method.key] = name
        for(c in method.cls.allChildClasses) {
            val key = "${c.key}.${method.name}${method.desc}"
            mappings[key] = name
        }
    }

    fun mapField(field: FieldNode, name: String) {
        if(mappings.containsKey(field.key)) return
        mappings[field.key] = name
        for(c in field.cls.allChildClasses) {
            val key = "${c.key}.${field.name}"
            mappings[key] = name
        }
    }

    operator fun get(oldName: String) = mappings[oldName]

    fun toRemapper(): SimpleRemapper {
        return SimpleRemapper(mappings)
    }
}