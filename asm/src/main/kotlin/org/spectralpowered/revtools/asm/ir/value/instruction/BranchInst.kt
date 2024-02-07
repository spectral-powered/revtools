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

class BranchInst internal constructor(
    cond: Value,
    type: Type,
    trueSuccessor: BasicBlock,
    falseSuccessor: BasicBlock,
    ctx: UsageContext
) : TerminateInst(UndefinedName(), type, mutableListOf(cond), mutableListOf(trueSuccessor, falseSuccessor), ctx) {

    val cond: Value
        get() = ops[0]

    val trueSuccessor: BasicBlock
        get() = internalSuccessors[0]

    val falseSuccessor: BasicBlock
        get() = internalSuccessors[1]

    override fun print() = "if ($cond) goto ${trueSuccessor.name} else ${falseSuccessor.name}"
    override fun clone(ctx: UsageContext): Instruction = BranchInst(cond, type, trueSuccessor, falseSuccessor, ctx)
}
