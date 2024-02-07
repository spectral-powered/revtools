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

import org.spectralpowered.revtools.asm.ir.Method
import org.spectralpowered.revtools.asm.ir.MethodDescriptor
import org.spectralpowered.revtools.asm.ir.value.Name
import org.spectralpowered.revtools.asm.ir.value.Slot
import org.spectralpowered.revtools.asm.ir.value.UsageContext
import org.spectralpowered.revtools.asm.ir.value.Value

data class Handle(val tag: Int, val method: Method, val isInterface: Boolean) {
    override fun toString() = "@$method"
}

@Suppress("MemberVisibilityCanBePrivate")
class InvokeDynamicInst(
    name: Name,
    val methodName: String,
    val methodDescriptor: MethodDescriptor,
    val bootstrapMethod: Handle,
    val bootstrapMethodArgs: List<Any>,
    operands: List<Value>,
    ctx: UsageContext
) : Instruction(name, methodDescriptor.returnType, operands.toMutableList(), ctx) {
    val args: List<Value> get() = ops

    constructor(
        methodName: String,
        methodDescriptor: MethodDescriptor,
        bootstrapMethod: Handle,
        bootstrapMethodArgs: List<Any>,
        operands: List<Value>,
        ctx: UsageContext
    ) : this(Slot(), methodName, methodDescriptor, bootstrapMethod, bootstrapMethodArgs, operands, ctx)

    override fun print(): String = buildString {
        append("$name = invokeDynamic $methodDescriptor $bootstrapMethod(${bootstrapMethodArgs.joinToString(", ")})")
    }

    override fun clone(ctx: UsageContext): Instruction =
        InvokeDynamicInst(methodName, methodDescriptor, bootstrapMethod, bootstrapMethodArgs, args, ctx)
}
