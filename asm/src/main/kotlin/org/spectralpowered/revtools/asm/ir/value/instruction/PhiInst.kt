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
import org.spectralpowered.revtools.asm.ir.value.*
import org.spectralpowered.revtools.asm.type.Type

class PhiInst internal constructor(
    name: Name,
    type: Type,
    predecessors: List<BasicBlock>,
    operands: List<Value>,
    ctx: UsageContext
) : Instruction(name, type, operands.toMutableList(), ctx), BlockUser {
    private val predecessorBlocks = predecessors.toMutableList()

    init {
        with(ctx) {
            incomings.keys.forEach { it.addUser(this@PhiInst) }
        }
    }

    val predecessors: List<BasicBlock>
        get() = predecessorBlocks

    val incomingValues: List<Value>
        get() = ops

    val incomings: Map<BasicBlock, Value>
        get() = predecessors.zip(ops).toMap()

    override fun print() = buildString {
        append("$name = phi {")
        append(predecessors.indices
            .joinToString(separator = "; ") {
                "${predecessors[it].name} -> ${incomingValues[it]}"
            })
        append("}")
    }

    override fun clone(ctx: UsageContext): Instruction = PhiInst(name.clone(), type, predecessors, operands, ctx)

    override fun replaceUsesOf(ctx: BlockUsageContext, from: UsableBlock, to: UsableBlock) = with(ctx) {
        for (index in predecessorBlocks.indices) {
            if (predecessorBlocks[index] == from) {
                predecessorBlocks[index].removeUser(this@PhiInst)
                predecessorBlocks[index] = to.get()
                to.addUser(this@PhiInst)
            }
        }
    }

    @Suppress("unused")
    fun replaceAllUsesWith(ctx: UsageContext, to: UsableValue) = with(ctx) {
        this@PhiInst.replaceAllUsesWith(to)
        if (to is BlockUser) {
            for (it in predecessorBlocks) {
                it.removeUser(this@PhiInst)
                it.addUser(to)
            }
        }
    }

    override fun clearBlockUses(ctx: BlockUsageContext) = with(ctx) {
        predecessors.forEach {
            it.removeUser(this@PhiInst)
        }
    }
}
