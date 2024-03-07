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

package org.spectralpowered.revtools.expr.impl

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.LdcInsnNode

open class ConstExpr(insn: AbstractInsnNode, index: Int, size: Int) : E0Expr(insn, index, size) {

    val number: Number? get() = when(opcode) {
        in ICONST_M1..ICONST_5 -> opcode - ICONST_0
        in LCONST_0..LCONST_1 -> (opcode - LCONST_0).toLong()
        in FCONST_0 .. FCONST_2 -> (opcode - FCONST_0).toFloat()
        in DCONST_0 .. DCONST_1 -> (opcode - DCONST_0).toDouble()
        BIPUSH -> (insn as IntInsnNode).operand.toByte()
        SIPUSH -> (insn as IntInsnNode).operand.toShort()
        LDC -> {
            if((insn as LdcInsnNode).cst is Number) {
                (insn as LdcInsnNode).cst as Number
            } else {
                null
            }
        }
        else -> null
    }
}