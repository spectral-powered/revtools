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

import org.spectralpowered.revtools.asm.ir.Field
import org.spectralpowered.revtools.asm.ir.value.UndefinedName
import org.spectralpowered.revtools.asm.ir.value.UsageContext
import org.spectralpowered.revtools.asm.ir.value.Value
import org.spectralpowered.revtools.asm.helper.assert.asserted

@Suppress("MemberVisibilityCanBePrivate")
class FieldStoreInst : Instruction {
    val field: Field
    val isStatic: Boolean

    internal constructor(field: Field, value: Value, ctx: UsageContext)
            : super(UndefinedName(), field.type, mutableListOf(value), ctx) {
        this.field = field
        isStatic = true
    }

    internal constructor(owner: Value, field: Field, value: Value, ctx: UsageContext)
            : super(UndefinedName(), field.type, mutableListOf(owner, value), ctx) {
        this.field = field
        isStatic = false
    }

    val hasOwner: Boolean
        get() = !isStatic

    val owner: Value
        get() = asserted(hasOwner) { ops[0] }

    val value: Value
        get() = if (hasOwner) ops[1] else ops[0]

    override fun print(): String {
        val sb = StringBuilder()
        if (hasOwner) sb.append("$owner.")
        else sb.append("${field.klass.name}.")
        sb.append("${field.name} = $value")
        return sb.toString()
    }

    override fun clone(ctx: UsageContext): Instruction = when {
        isStatic -> FieldStoreInst(field, value, ctx)
        else -> FieldStoreInst(owner, field, value, ctx)
    }
}
