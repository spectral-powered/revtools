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
import org.spectralpowered.revtools.asm.helper.assert.ktassert
import org.spectralpowered.revtools.asm.ir.BasicBlock
import org.spectralpowered.revtools.asm.ir.BodyBlock
import org.spectralpowered.revtools.asm.ir.MethodBody
import org.spectralpowered.revtools.asm.ir.value.UsageContext
import org.spectralpowered.revtools.asm.ir.value.Value
import org.spectralpowered.revtools.asm.ir.value.ValueFactory
import org.spectralpowered.revtools.asm.ir.value.instruction.Instruction
import org.spectralpowered.revtools.asm.ir.value.instruction.InstructionBuilder
import org.spectralpowered.revtools.asm.ir.value.instruction.InstructionFactory
import org.spectralpowered.revtools.asm.ir.value.instruction.ReturnInst
import org.spectralpowered.revtools.asm.type.Integer
import org.spectralpowered.revtools.asm.type.Type
import org.spectralpowered.revtools.asm.type.TypeFactory
import org.spectralpowered.revtools.asm.type.commonSupertype
import org.spectralpowered.revtools.asm.visitor.MethodVisitor
import kotlin.math.abs

class RetvalBuilder(override val group: ClassGroup, override val ctx: UsageContext) : MethodVisitor,
    InstructionBuilder {
    private val returnValues = linkedMapOf<BasicBlock, ReturnInst>()
    override val instructions: InstructionFactory
        get() = group.instruction
    override val types: TypeFactory
        get() = group.type
    override val values: ValueFactory
        get() = group.value

    override fun cleanup() {
        returnValues.clear()
    }

    override fun visitReturnInst(inst: ReturnInst) {
        val bb = inst.parent
        returnValues[bb] = inst
    }

    override fun visitBody(body: MethodBody): Unit = with(ctx) {
        super.visitBody(body)
        if (returnValues.size <= 1) return

        val returnBlock = BodyBlock("bb.return")

        val incomings = linkedMapOf<BasicBlock, Value>()
        for ((bb, returnInst) in returnValues) {
            bb.remove(returnInst)
            returnInst.clearAllUses()
            bb.linkForward(returnBlock)
            if (returnInst.hasReturnValue)
                incomings[bb] = returnInst.returnValue

            val jump = goto(returnBlock)
            jump.location = returnInst.location
            bb += jump
        }

        val instructions = arrayListOf<Instruction>()
        val returnType = body.method.returnType
        val returnInstruction = when {
            returnType.isVoid -> `return`()
            else -> {
                val type = commonSupertype(incomings.values.mapTo(mutableSetOf()) { it.type }) ?: returnType

                val returnValuePhi = phi("retval", type, incomings)
                instructions.add(returnValuePhi)

                val returnValue = when (type) {
                    returnType -> returnValuePhi
                    is Integer -> {
                        ktassert(
                            returnType is Integer,
                            "Return value type is integral and method return type is $returnType"
                        )

                        // if return type is Int and return value type is Long (or vice versa), we need casting
                        // otherwise it's fine
                        if (abs(type.bitSize - returnType.bitSize) >= Type.WORD) {
                            val retvalCasted = returnValuePhi.cast("retval.casted", returnType)
                            instructions.add(retvalCasted)
                            retvalCasted
                        } else {
                            returnValuePhi
                        }
                    }

                    else -> {
                        val retvalCasted = returnValuePhi.cast("retval.casted", returnType)
                        instructions.add(retvalCasted)
                        retvalCasted
                    }
                }

                `return`(returnValue)
            }
        }
        instructions.add(returnInstruction)
        returnBlock.addAll(*instructions.toTypedArray())
        body.add(returnBlock)
    }
}
