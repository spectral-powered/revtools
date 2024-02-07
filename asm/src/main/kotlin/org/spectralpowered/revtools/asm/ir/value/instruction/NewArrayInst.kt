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

import org.spectralpowered.revtools.asm.ir.value.Name
import org.spectralpowered.revtools.asm.ir.value.UsageContext
import org.spectralpowered.revtools.asm.ir.value.Value
import org.spectralpowered.revtools.asm.type.ArrayType
import org.spectralpowered.revtools.asm.type.Type
import org.spectralpowered.revtools.asm.helper.assert.ktassert

@Suppress("MemberVisibilityCanBePrivate")
class NewArrayInst internal constructor(
    name: Name,
    type: Type,
    dimensions: List<Value>,
    ctx: UsageContext
) : Instruction(name, type, dimensions.toMutableList(), ctx) {
    val component: Type

    val dimensions: List<Value>
        get() = ops

    val numDimensions: Int
        get() = ops.size

    init {
        var current = type
        repeat(numDimensions) {
            ktassert(current is ArrayType)
            current = (current as ArrayType).component
        }
        this.component = current
    }

    override fun print(): String {
        val sb = StringBuilder()
        sb.append("$name = new ${component.name}")
        dimensions.forEach {
            sb.append("[$it]")
        }
        return sb.toString()
    }

    override fun clone(ctx: UsageContext): Instruction = NewArrayInst(name.clone(), type, ops, ctx)
}
