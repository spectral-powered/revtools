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

import org.spectralpowered.revtools.asm.ir.value.UndefinedName
import org.spectralpowered.revtools.asm.ir.value.UsageContext
import org.spectralpowered.revtools.asm.ir.value.Value
import org.spectralpowered.revtools.asm.type.ArrayType
import org.spectralpowered.revtools.asm.type.Type
import org.spectralpowered.revtools.asm.helper.assert.unreachable

class ArrayStoreInst internal constructor(
    arrayRef: Value,
    type: Type,
    index: Value,
    value: Value,
    ctx: UsageContext
) : Instruction(UndefinedName(), type, mutableListOf(arrayRef, index, value), ctx) {

    val arrayRef: Value
        get() = ops[0]

    val index: Value
        get() = ops[1]

    val value: Value
        get() = ops[2]

    val arrayComponent: Type
        get() = (arrayRef.type as? ArrayType)?.component ?: unreachable("Non-array ref in array store")

    override fun print() = "$arrayRef[$index] = $value"
    override fun clone(ctx: UsageContext): Instruction = ArrayStoreInst(arrayRef, type, index, value, ctx)
}
