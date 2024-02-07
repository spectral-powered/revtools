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
import org.spectralpowered.revtools.asm.ir.BasicBlock
import org.spectralpowered.revtools.asm.ir.CatchBlock
import org.spectralpowered.revtools.asm.ir.MethodBody
import org.spectralpowered.revtools.asm.ir.value.UsageContext
import org.spectralpowered.revtools.asm.ir.value.instruction.PhiInst
import org.spectralpowered.revtools.asm.visitor.MethodVisitor

class CfgOptimizer(override val group: ClassGroup, val ctx: UsageContext) : MethodVisitor {
    override fun cleanup() {}

    private val BasicBlock.canBeOptimized: Boolean
        get() {
            if (this.isEmpty) return false
            if (this.size > 1) return false
            if (this.successors.size != 1) return false
            if (this.predecessors.size != 1) return false
            if (this.predecessors.first().successors.size != 1) return false
            if (this.successors.first().predecessors.size != 1) return false

            val successor = this.successors.first()
            return this.handlers == successor.handlers
        }

    override fun visitBody(body: MethodBody) = with(ctx) {
        val phiMappings = mutableMapOf<PhiInst, MutableMap<BasicBlock, BasicBlock?>>()
        for (block in body.basicBlocks.toList()) {
            if (!block.canBeOptimized) continue
            val successor = block.successors.first()
            val predecessor = block.predecessors.first()

            val handlers = block.handlers
            for (catch in handlers) {
                catch.removeThrower(block)
            }

            val blockPhiUsers = block.users.filterIsInstance<PhiInst>()
            for (phi in blockPhiUsers) {
                val parent = phi.parentUnsafe ?: continue

                when (parent) {
                    is CatchBlock -> when (block) {
                        in parent.entries -> phiMappings.getOrPut(phi, ::mutableMapOf)[block] = predecessor
                        else -> phiMappings.getOrPut(phi, ::mutableMapOf)[block] = null
                    }

                    else -> phiMappings.getOrPut(phi, ::mutableMapOf)[block] = predecessor
                }
            }

            body.remove(block)
            for (inst in block.toList()) {
                block.remove(inst)
                inst.clearAllUses()
            }
            predecessor.removeSuccessor(block)
            block.removeSuccessor(successor)
            predecessor.addSuccessor(successor)
            successor.addPredecessor(predecessor)
            block.replaceAllUsesWith(successor)
        }


        for ((phi, mappings) in phiMappings) {
            val parent = phi.parentUnsafe ?: continue
            val incomings = phi.incomings.mapNotNull {
                if (it.key in mappings) {
                    if (mappings[it.key] == null) null
                    else mappings[it.key]!! to it.value
                } else it.key to it.value
            }.toMap()
            val newPhi = inst(group) { phi(phi.type, incomings) }

            phi.replaceAllUsesWith(newPhi)
            parent.replace(phi, newPhi)
            phi.clearAllUses()
        }
    }
}
