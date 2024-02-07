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

package org.spectralpowered.revtools.asm.ir.value.instruction

import org.spectralpowered.revtools.asm.ir.BasicBlock
import org.spectralpowered.revtools.asm.ir.value.UndefinedName
import org.spectralpowered.revtools.asm.ir.value.UsageContext
import org.spectralpowered.revtools.asm.ir.value.Value
import org.spectralpowered.revtools.asm.type.Type

class SwitchInst internal constructor(
    key: Value,
    type: Type,
    default: BasicBlock,
    operands: List<Value>,
    predecessors: List<BasicBlock>,
    ctx: UsageContext
) : TerminateInst(
    UndefinedName(),
    type,
    mutableListOf(key).also { it.addAll(operands) },
    mutableListOf(default).also { it.addAll(predecessors) },
    ctx
) {

    val key: Value
        get() = ops[0]

    val default: BasicBlock
        get() = internalSuccessors[0]

    val branches: Map<Value, BasicBlock>
        get() = ops.drop(1).zip(internalSuccessors.drop(1)).toMap()

    override fun print(): String {
        val sb = StringBuilder()
        sb.append("switch ($key) {")
        branches.forEach { sb.append("${it.key} -> ${it.value.name}; ") }
        sb.append("else -> ${default.name}}")
        return sb.toString()
    }

    override fun clone(ctx: UsageContext): Instruction =
        SwitchInst(key, type, default, operands, successors.drop(1), ctx)
}
