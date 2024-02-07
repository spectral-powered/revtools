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
import org.spectralpowered.revtools.asm.ir.value.IntConstant
import org.spectralpowered.revtools.asm.ir.value.UndefinedName
import org.spectralpowered.revtools.asm.ir.value.UsageContext
import org.spectralpowered.revtools.asm.ir.value.Value
import org.spectralpowered.revtools.asm.type.Type

@Suppress("MemberVisibilityCanBePrivate")
class TableSwitchInst internal constructor(
    type: Type,
    index: Value,
    min: Value,
    max: Value,
    default: BasicBlock,
    branches: List<BasicBlock>,
    ctx: UsageContext
) : TerminateInst(
    UndefinedName(),
    type,
    mutableListOf(index, min, max),
    mutableListOf(default).also { it.addAll(branches) },
    ctx
) {

    val index: Value
        get() = ops[0]

    val min: Value
        get() = ops[1]

    val max: Value
        get() = ops[2]

    val default get() = internalSuccessors[0]
    val branches get() = internalSuccessors.drop(1)

    val range: IntRange
        get() = (min as IntConstant).value..(max as IntConstant).value

    override fun print() = buildString {
        append("tableswitch ($index) {")
        for ((index, successor) in range.zip(branches)) {
            append("$index -> ${successor.name}; ")
        }
        append("else -> ${default.name}}")
    }

    override fun clone(ctx: UsageContext): Instruction =
        TableSwitchInst(type, index, min, max, default, branches, ctx)
}
