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

import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.spectralpowered.revtools.ClassPool
import org.spectralpowered.revtools.deobfuscator.bytecode.BytecodeTransformer
import org.tinylog.kotlin.Logger

class DeadCodeTransformer : BytecodeTransformer {

    private var count = 0

    override fun run(pool: ClassPool) {
        for(cls in pool.classes) {
            for(method in cls.methods) {
                val insns = method.instructions.toArray()
                val frames = Analyzer(BasicInterpreter()).analyze(cls.name, method)
                for(i in insns.indices) {
                    val frame = frames[i]
                    if(frame == null) {
                        if(insns[i] is LabelNode) count++
                        method.instructions.remove(insns[i])
                    }
                }
            }
        }
    }

    override fun postRun() {
        Logger.info("Removed $count dead-code blocks.")
    }
}