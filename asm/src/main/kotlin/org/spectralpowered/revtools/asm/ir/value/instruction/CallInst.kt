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

import org.spectralpowered.revtools.asm.ir.Class
import org.spectralpowered.revtools.asm.ir.Method
import org.spectralpowered.revtools.asm.ir.value.Name
import org.spectralpowered.revtools.asm.ir.value.UndefinedName
import org.spectralpowered.revtools.asm.ir.value.UsageContext
import org.spectralpowered.revtools.asm.ir.value.Value
import org.spectralpowered.revtools.asm.type.VoidType
import org.spectralpowered.revtools.asm.helper.assert.asserted
import org.spectralpowered.revtools.asm.helper.assert.ktassert

@Suppress("MemberVisibilityCanBePrivate")
class CallInst : Instruction {
    val opcode: CallOpcode
    val method: Method
    val klass: Class
    val isStatic: Boolean

    val callee: Value
        get() = asserted(!isStatic) { ops[0] }

    val args: List<Value>
        get() = when {
            isStatic -> ops.toList()
            else -> ops.drop(1)
        }

    internal constructor(opcode: CallOpcode, method: Method, klass: Class, args: List<Value>, ctx: UsageContext)
            : super(UndefinedName(), method.returnType, args.toMutableList(), ctx) {
        this.opcode = opcode
        this.method = method
        this.klass = klass
        this.isStatic = true
    }

    internal constructor(
        opcode: CallOpcode,
        method: Method,
        klass: Class,
        obj: Value,
        args: List<Value>,
        ctx: UsageContext
    ) : super(UndefinedName(), method.returnType, mutableListOf(obj).also { it.addAll(args) }, ctx) {
        this.opcode = opcode
        this.method = method
        this.klass = klass
        this.isStatic = false
    }

    internal constructor(
        opcode: CallOpcode,
        name: Name,
        method: Method,
        klass: Class,
        args: List<Value>,
        ctx: UsageContext
    ) : super(name, method.returnType, args.toMutableList(), ctx) {
        ktassert(
            (method.returnType is VoidType && name is UndefinedName) || method.returnType !is VoidType,
            "named CallInst should not have type `VoidType`"
        )
        this.opcode = opcode
        this.method = method
        this.klass = klass
        this.isStatic = true
    }

    internal constructor(
        opcode: CallOpcode,
        name: Name,
        method: Method,
        klass: Class,
        obj: Value,
        args: List<Value>,
        ctx: UsageContext
    ) : super(name, method.returnType, mutableListOf(obj).also { it.addAll(args) }, ctx) {
        ktassert(
            (method.returnType is VoidType && name is UndefinedName) || method.returnType !is VoidType,
            "named CallInst should not have type `VoidType`"
        )
        this.opcode = opcode
        this.method = method
        this.klass = klass
        this.isStatic = false
    }

    override fun print(): String {
        val sb = StringBuilder()
        if (name !is UndefinedName) sb.append("$name = ")

        sb.append("$opcode ")
        if (isStatic) sb.append(klass.name)
        else sb.append(callee.name)
        sb.append(".${method.name}(")
        sb.append(args.joinToString())
        sb.append(")")
        return sb.toString()
    }

    override fun clone(ctx: UsageContext): Instruction = when {
        isStatic -> CallInst(opcode, name.clone(), method, klass, args, ctx)
        else -> CallInst(opcode, name.clone(), method, klass, callee, args, ctx)
    }
}
