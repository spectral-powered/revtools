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

package org.spectralpowered.revtools.asm.builder.cfg

import org.spectralpowered.revtools.asm.ClassGroup
import org.spectralpowered.revtools.asm.helper.assert.unreachable
import org.spectralpowered.revtools.asm.ir.value.UsageContext
import org.spectralpowered.revtools.asm.ir.value.ValueFactory
import org.spectralpowered.revtools.asm.ir.value.instruction.*
import org.spectralpowered.revtools.asm.type.ArrayType
import org.spectralpowered.revtools.asm.type.BoolType
import org.spectralpowered.revtools.asm.type.Integer
import org.spectralpowered.revtools.asm.type.TypeFactory
import org.spectralpowered.revtools.asm.visitor.MethodVisitor

@Suppress("unused")
class BoolValueAdapter(
    override val group: ClassGroup,
    override val ctx: UsageContext
) : MethodVisitor, InstructionBuilder {
    override val instructions: InstructionFactory
        get() = group.instruction
    override val types: TypeFactory
        get() = group.type
    override val values: ValueFactory
        get() = group.value

    override fun cleanup() {}

    override fun visitArrayStoreInst(inst: ArrayStoreInst) {
        val bb = inst.parent

        val arrayType = inst.arrayRef.type as? ArrayType
            ?: unreachable("Non-array type of array store reference")

        if (arrayType.component is BoolType && inst.value.type is Integer) {
            val cast = inst.value `as` types.boolType
            bb.insertBefore(inst, cast)
            inst.replaceUsesOf(ctx, from = inst.value, to = cast)
        }
    }

    override fun visitFieldStoreInst(inst: FieldStoreInst) {
        val bb = inst.parent

        if (inst.type is BoolType && inst.value.type is Integer) {
            val cast = inst.value `as` types.boolType
            bb.insertBefore(inst, cast)
            inst.replaceUsesOf(ctx, from = inst.value, to = cast)
        }
    }

    override fun visitReturnInst(inst: ReturnInst) {
        val bb = inst.parent
        val method = bb.method

        if (method.returnType is BoolType && inst.returnValue.type !is BoolType) {
            val cast = inst.returnValue `as` types.boolType
            bb.insertBefore(inst, cast)
            inst.replaceUsesOf(ctx, from = inst.returnValue, to = cast)
        }
    }
}
