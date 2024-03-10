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
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.spectralpowered.revtools.asm.node.cls
import org.spectralpowered.revtools.asm.node.isBodyEmpty
import org.spectralpowered.revtools.deobfuscator.bytecode.Transformer
import org.tinylog.kotlin.Logger

class DeadCodeTransformer : Transformer() {

    private var count = 0

    override fun transformMethod(method: MethodNode): Boolean {
        var changed: Boolean
        do {
            changed = false
            val frames = Analyzer(BasicInterpreter()).analyze(method.cls.name, method)
            val insns = method.instructions.iterator()
            var index = 0
            for(insn in insns) {
                if(frames[index++] != null || insn is LabelNode) {
                    continue
                }
                insns.remove()
                changed = true
            }
            changed = changed or method.tryCatchBlocks.removeIf { it.isBodyEmpty() }
            if(changed) count++
        } while (changed)
        return false
    }

    override fun onComplete() {
        Logger.info("Removed $count dead instructions from methods.")
    }
}