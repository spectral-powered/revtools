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

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.spectralpowered.revtools.asm.ClassPool
import org.spectralpowered.revtools.asm.node.isStatic
import org.spectralpowered.revtools.deobfuscator.bytecode.Transformer
import org.spectralpowered.revtools.deobfuscator.bytecode.remap.NameMap
import org.tinylog.kotlin.Logger
class RenameTransformer : Transformer() {

    private lateinit var nameMapper: NameMap

    private var classCount = 0
    private var memberMethodCount = 0
    private var staticMethodCount = 0
    private var memberFieldCount = 0
    private var staticFieldCount = 0
    private var totalMethodCount = 0
    private var totalFieldCount = 0

    override fun transform(pool: ClassPool) {
        nameMapper = NameMap(pool)
        super.transform(pool)
    }

    override fun transformClass(cls: ClassNode): Boolean {
        if(cls.name.isObfuscatedName()) {
            nameMapper.renameClass(cls, "class${++classCount}")
        }
        return false
    }

    override fun transformMethod(method: MethodNode): Boolean {
        if(method.name.isObfuscatedName() && !nameMapper.hasMethodMapping(method)) {
            val newName = when {
                method.isStatic() -> "staticMethod${++staticMethodCount}"
                else -> "method${++memberMethodCount}"
            }
            nameMapper.renameMethod(method, newName)
            totalMethodCount++
        }
        return false
    }

    override fun transformField(field: FieldNode): Boolean {
        if(field.name.isObfuscatedName() && !nameMapper.hasFieldMapping(field)) {
            val newName = when {
                field.isStatic() -> "staticField${++staticFieldCount}"
                else -> "field${++memberFieldCount}"
            }
            nameMapper.renameField(field, newName)
            totalFieldCount++
        }
        return false
    }

    override fun postTransform(): Boolean {
        pool.remap(nameMapper)
        return false
    }

    override fun onComplete() {
        Logger.info("Renamed Classes: $classCount, Methods: $totalMethodCount, Fields: $totalFieldCount)")
    }

    private fun String.isObfuscatedName(): Boolean {
        return length <= 2 || (length == 3 && this !in listOf("add", "get", "set", "put", "run"))
    }
}