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

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.spectralpowered.revtools.asm.node.cls
import org.spectralpowered.revtools.deobfuscator.bytecode.Transformer
import org.tinylog.kotlin.Logger

class InvokeSpecialTransformer : Transformer() {

    private var count = 0

    override fun transformMethod(method: MethodNode): Boolean {
        if((method.cls.access and ACC_FINAL) == 0) return false

        for(insn in method.instructions) {
            if(insn !is MethodInsnNode || insn.opcode != INVOKESPECIAL) {
                continue
            } else if(insn.name == "<init>") {
                continue
            } else if(insn.owner != method.cls.name) {
                continue
            }

            insn.opcode = INVOKEVIRTUAL
            count++
        }
        return false
    }

    override fun onComplete() {
        Logger.info("Updated $count INVOKESPECIAL instructions.")
    }
}