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

import org.spectralpowered.revtools.asm.helper.assert.asserted
import org.spectralpowered.revtools.asm.ir.BasicBlock
import org.spectralpowered.revtools.asm.ir.Location
import org.spectralpowered.revtools.asm.ir.value.*
import org.spectralpowered.revtools.asm.type.Type

abstract class Instruction internal constructor(
    name: Name,
    type: Type,
    protected val ops: MutableList<Value>,
    ctx: UsageContext
) : Value(name, type), ValueUser, Iterable<Value> {

    internal var parentUnsafe: BasicBlock? = null
    var location = Location()
        internal set

    val parent get() = asserted(hasParent) { parentUnsafe!! }
    val hasParent get() = parentUnsafe != null

    open val isTerminate = false

    val operands: List<Value>
        get() = ops

    init {
        with(ctx) {
            ops.forEach { it.addUser(this@Instruction) }
        }
    }


    abstract fun print(): String
    override fun iterator(): Iterator<Value> = ops.iterator()

    override fun replaceUsesOf(ctx: ValueUsageContext, from: UsableValue, to: UsableValue) = with(ctx) {
        for (index in ops.indices) {
            if (ops[index] == from) {
                ops[index].removeUser(this@Instruction)
                ops[index] = to.get()
                to.addUser(this@Instruction)
            }
        }
    }

    abstract fun clone(ctx: UsageContext): Instruction
    open fun update(ctx: UsageContext, remapping: Map<Value, Value> = mapOf(), loc: Location = location): Instruction {
        val new = clone(ctx)
        remapping.forEach { (from, to) -> new.replaceUsesOf(ctx, from, to) }
        new.location = loc
        return new
    }

    override fun clearValueUses(ctx: ValueUsageContext) = with(ctx) {
        ops.forEach {
            it.removeUser(this@Instruction)
        }
    }
}

abstract class TerminateInst(
    name: Name,
    type: Type,
    operands: MutableList<Value>,
    protected val internalSuccessors: MutableList<BasicBlock>,
    ctx: UsageContext
) : Instruction(name, type, operands, ctx), BlockUser {

    val successors: List<BasicBlock>
        get() = internalSuccessors

    override val isTerminate = true

    init {
        with(ctx) {
            internalSuccessors.forEach { it.addUser(this@TerminateInst) }
        }
    }

    override fun replaceUsesOf(ctx: BlockUsageContext, from: UsableBlock, to: UsableBlock) = with(ctx) {
        for (index in internalSuccessors.indices) {
            if (internalSuccessors[index] == from) {
                internalSuccessors[index].removeUser(this@TerminateInst)
                internalSuccessors[index] = to.get()
                to.addUser(this@TerminateInst)
            }
        }
    }

    override fun clearBlockUses(ctx: BlockUsageContext) = with(ctx) {
        internalSuccessors.forEach {
            it.removeUser(this@TerminateInst)
        }
    }
}
