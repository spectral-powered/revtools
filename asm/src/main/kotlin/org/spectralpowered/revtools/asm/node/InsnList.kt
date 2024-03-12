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

package org.spectralpowered.revtools.asm.node

import org.objectweb.asm.tree.AbstractInsnNode
import org.spectralpowered.revtools.asm.stackMetadata

private val ANY_INSN = { _: AbstractInsnNode -> true }

fun AbstractInsnNode.getExpression(
    filter: (AbstractInsnNode) -> Boolean = ANY_INSN,
    initialHeight: Int = 0
): List<AbstractInsnNode>? {
    val expr = mutableListOf<AbstractInsnNode>()

    var height = initialHeight
    var insn: AbstractInsnNode? = this
    do {
        val (pops, pushes) = insn!!.stackMetadata
        expr.add(insn)
        if(insn !== this || initialHeight != 0) {
            height -= pushes
        }
        height += pops

        if(height == 0) {
            return expr.asReversed()
        }

        insn = insn.previous
    } while(insn != null && insn.isSequential && filter(insn))

    return null
}