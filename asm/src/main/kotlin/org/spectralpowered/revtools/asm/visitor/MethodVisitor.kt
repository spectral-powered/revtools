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

package org.spectralpowered.revtools.asm.visitor

import org.spectralpowered.revtools.asm.ir.BasicBlock
import org.spectralpowered.revtools.asm.ir.Method
import org.spectralpowered.revtools.asm.ir.MethodBody
import org.spectralpowered.revtools.asm.ir.value.instruction.ArrayLoadInst
import org.spectralpowered.revtools.asm.ir.value.instruction.ArrayStoreInst
import org.spectralpowered.revtools.asm.ir.value.instruction.BinaryInst
import org.spectralpowered.revtools.asm.ir.value.instruction.BranchInst
import org.spectralpowered.revtools.asm.ir.value.instruction.CallInst
import org.spectralpowered.revtools.asm.ir.value.instruction.CastInst
import org.spectralpowered.revtools.asm.ir.value.instruction.CatchInst
import org.spectralpowered.revtools.asm.ir.value.instruction.CmpInst
import org.spectralpowered.revtools.asm.ir.value.instruction.EnterMonitorInst
import org.spectralpowered.revtools.asm.ir.value.instruction.ExitMonitorInst
import org.spectralpowered.revtools.asm.ir.value.instruction.FieldLoadInst
import org.spectralpowered.revtools.asm.ir.value.instruction.FieldStoreInst
import org.spectralpowered.revtools.asm.ir.value.instruction.InstanceOfInst
import org.spectralpowered.revtools.asm.ir.value.instruction.Instruction
import org.spectralpowered.revtools.asm.ir.value.instruction.InvokeDynamicInst
import org.spectralpowered.revtools.asm.ir.value.instruction.JumpInst
import org.spectralpowered.revtools.asm.ir.value.instruction.NewArrayInst
import org.spectralpowered.revtools.asm.ir.value.instruction.NewInst
import org.spectralpowered.revtools.asm.ir.value.instruction.PhiInst
import org.spectralpowered.revtools.asm.ir.value.instruction.ReturnInst
import org.spectralpowered.revtools.asm.ir.value.instruction.SwitchInst
import org.spectralpowered.revtools.asm.ir.value.instruction.TableSwitchInst
import org.spectralpowered.revtools.asm.ir.value.instruction.TerminateInst
import org.spectralpowered.revtools.asm.ir.value.instruction.ThrowInst
import org.spectralpowered.revtools.asm.ir.value.instruction.UnaryInst
import org.spectralpowered.revtools.asm.ir.value.instruction.UnknownValueInst
import org.spectralpowered.revtools.asm.ir.value.instruction.UnreachableInst
import org.spectralpowered.revtools.asm.helper.assert.unreachable

interface MethodVisitor : NodeVisitor {

    fun visit(method: Method) {
        super.visit(method)
        visitBody(method.body)
    }

    fun visitBody(body: MethodBody) {
        body.run {
            basicBlocks.toTypedArray().forEach { visitBasicBlock(it) }
        }
    }

    fun visitBasicBlock(bb: BasicBlock) {
        bb.instructions.toTypedArray().forEach { visitInstruction(it) }
    }

    fun visitInstruction(inst: Instruction) {
        when (inst) {
            is ArrayLoadInst -> visitArrayLoadInst(inst)
            is ArrayStoreInst -> visitArrayStoreInst(inst)
            is BinaryInst -> visitBinaryInst(inst)
            is CallInst -> visitCallInst(inst)
            is CastInst -> visitCastInst(inst)
            is CatchInst -> visitCatchInst(inst)
            is CmpInst -> visitCmpInst(inst)
            is EnterMonitorInst -> visitEnterMonitorInst(inst)
            is ExitMonitorInst -> visitExitMonitorInst(inst)
            is FieldLoadInst -> visitFieldLoadInst(inst)
            is FieldStoreInst -> visitFieldStoreInst(inst)
            is InstanceOfInst -> visitInstanceOfInst(inst)
            is InvokeDynamicInst -> visitInvokeDynamicInst(inst)
            is NewArrayInst -> visitNewArrayInst(inst)
            is NewInst -> visitNewInst(inst)
            is PhiInst -> visitPhiInst(inst)
            is UnaryInst -> visitUnaryInst(inst)
            is TerminateInst -> visitTerminateInst(inst)
            is UnknownValueInst -> visitUnknownValueInst(inst)
            else -> unreachable("Unknown instruction ${inst.print()}")
        }
    }

    fun visitTerminateInst(inst: TerminateInst) {
        when (inst) {
            is BranchInst -> visitBranchInst(inst)
            is JumpInst -> visitJumpInst(inst)
            is ReturnInst -> visitReturnInst(inst)
            is SwitchInst -> visitSwitchInst(inst)
            is TableSwitchInst -> visitTableSwitchInst(inst)
            is ThrowInst -> visitThrowInst(inst)
            is UnreachableInst -> visitUnreachableInst(inst)
            else -> unreachable("Unknown instruction ${inst.print()}")
        }
    }

    fun visitArrayLoadInst(inst: ArrayLoadInst) {}
    fun visitArrayStoreInst(inst: ArrayStoreInst) {}
    fun visitBinaryInst(inst: BinaryInst) {}
    fun visitBranchInst(inst: BranchInst) {}
    fun visitCallInst(inst: CallInst) {}
    fun visitCastInst(inst: CastInst) {}
    fun visitCatchInst(inst: CatchInst) {}
    fun visitCmpInst(inst: CmpInst) {}
    fun visitEnterMonitorInst(inst: EnterMonitorInst) {}
    fun visitExitMonitorInst(inst: ExitMonitorInst) {}
    fun visitFieldLoadInst(inst: FieldLoadInst) {}
    fun visitFieldStoreInst(inst: FieldStoreInst) {}
    fun visitInstanceOfInst(inst: InstanceOfInst) {}
    fun visitInvokeDynamicInst(inst: InvokeDynamicInst) {}
    fun visitNewArrayInst(inst: NewArrayInst) {}
    fun visitNewInst(inst: NewInst) {}
    fun visitPhiInst(inst: PhiInst) {}
    fun visitUnaryInst(inst: UnaryInst) {}
    fun visitJumpInst(inst: JumpInst) {}
    fun visitReturnInst(inst: ReturnInst) {}
    fun visitSwitchInst(inst: SwitchInst) {}
    fun visitTableSwitchInst(inst: TableSwitchInst) {}
    fun visitThrowInst(inst: ThrowInst) {}
    fun visitUnreachableInst(inst: UnreachableInst) {}
    fun visitUnknownValueInst(inst : UnknownValueInst) {}
}
