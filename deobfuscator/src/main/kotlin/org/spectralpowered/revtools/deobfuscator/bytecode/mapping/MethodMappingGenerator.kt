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

import org.objectweb.asm.tree.MethodNode
import org.spectralpowered.revtools.ClassPool
import org.spectralpowered.revtools.deobfuscator.bytecode.mapping.NameGenerator.Companion.isRenamable
import org.spectralpowered.revtools.isAbstract
import org.spectralpowered.revtools.isStatic
import org.spectralpowered.revtools.key
import org.spectralpowered.revtools.remap.NameMap

class MethodMappingGenerator(
    private val pool: ClassPool,
    private val nameMap: NameMap
) {
    private val nameGen = NameGenerator()
    private val mapping = hashMapOf<MethodNode, String>()

    fun generate(): Map<MethodNode, String> {
        for(method in pool.classes.flatMap { it.methods }.sortedBy { it.isAbstract() }) {
            if(!method.name.isRenamable()) continue
            mapping[method] = nameMap[method.key] ?: generateName(method)
        }
        mapping.forEach { (method, name) ->
            nameMap.mapMethod(method, name)
        }
        return mapping
    }

    private fun generateName(method: MethodNode): String {
        val name = method.name
        var mappedName = name
        mappedName = if(method.isAbstract()) {
            "vmethod"
        } else if(method.isStatic()) {
            "staticMethod"
        } else {
            "method"
        }
        return nameGen.generate(mappedName)
    }
}