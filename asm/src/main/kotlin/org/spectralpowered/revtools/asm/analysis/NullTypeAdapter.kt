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

package org.spectralpowered.revtools.asm.analysis

import org.spectralpowered.revtools.asm.ClassGroup
import org.spectralpowered.revtools.asm.AsmException
import org.spectralpowered.revtools.asm.ir.value.UsageContext
import org.spectralpowered.revtools.asm.ir.value.instruction.PhiInst
import org.spectralpowered.revtools.asm.type.Type
import org.spectralpowered.revtools.asm.type.commonSupertype
import org.spectralpowered.revtools.asm.visitor.MethodVisitor

class TypeMergeFailedException(val types: Set<Type>) : AsmException()

class NullTypeAdapter(override val group: ClassGroup, val ctx: UsageContext) : MethodVisitor {
    override fun cleanup() {}

    override fun visitPhiInst(inst: PhiInst) = with(ctx) {
        if (inst.type == types.nullType) {
            val incomingTypes = inst.incomingValues.mapTo(mutableSetOf()) { it.type }
            val actualType = commonSupertype(incomingTypes) ?: throw TypeMergeFailedException(incomingTypes)
            val newPhi = inst(group) { phi(actualType, inst.incomings) }
            inst.parent.insertBefore(inst, newPhi)
            inst.replaceAllUsesWith(newPhi)
            inst.clearAllUses()
            inst.parent -= inst
        }
    }
}
