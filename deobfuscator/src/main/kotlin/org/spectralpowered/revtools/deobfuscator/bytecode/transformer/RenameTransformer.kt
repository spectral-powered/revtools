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

import org.spectralpowered.revtools.ClassPool
import org.spectralpowered.revtools.deobfuscator.bytecode.BytecodeTransformer
import org.spectralpowered.revtools.deobfuscator.bytecode.mapping.ClassMappingGenerator
import org.spectralpowered.revtools.deobfuscator.bytecode.mapping.FieldMappingGenerator
import org.spectralpowered.revtools.deobfuscator.bytecode.mapping.MethodMappingGenerator
import org.spectralpowered.revtools.remap.NameMap
import org.tinylog.kotlin.Logger

class RenameTransformer : BytecodeTransformer {

    private val nameMap = NameMap()

    private var classCount = 0
    private var methodCount = 0
    private var fieldCount = 0

    override fun run(pool: ClassPool) {
        generateMappings(pool)
        applyMappings(pool)
    }

    override fun postRun() {
        Logger.info("Renamed $classCount classes.")
        Logger.info("Renamed $methodCount methods.")
        Logger.info("Renamed $fieldCount fields.")
    }

    private fun generateMappings(pool: ClassPool) {
        val classes = ClassMappingGenerator(pool, nameMap).generate()
        val methods = MethodMappingGenerator(pool, nameMap).generate()
        val fields = FieldMappingGenerator(pool, nameMap).generate()
        classCount = classes.size
        methodCount = methods.size
        fieldCount = fields.size
    }

    private fun applyMappings(pool: ClassPool) {
        pool.remap(nameMap)
    }
}