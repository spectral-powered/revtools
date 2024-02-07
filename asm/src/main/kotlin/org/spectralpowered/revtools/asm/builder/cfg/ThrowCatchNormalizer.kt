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
import org.spectralpowered.revtools.asm.ir.MethodBody
import org.spectralpowered.revtools.asm.ir.value.UsageContext
import org.spectralpowered.revtools.asm.visitor.MethodVisitor

class ThrowCatchNormalizer(override val group: ClassGroup, val ctx: UsageContext) : MethodVisitor {
    override fun cleanup() {}

    override fun visitBody(body: MethodBody) = with(ctx) {
        for (block in body.basicBlocks) {
            if (block.size > 1) continue
            if (block.successors.size != 1) continue
            if (block.predecessors.size != 1) continue

            val successor = block.successors.first()
            val predecessor = block.predecessors.first()
            if (predecessor.handlers != successor.handlers) continue
            for (handler in successor.handlers intersect predecessor.handlers) {
                block.linkThrowing(handler)
            }
        }
    }
}
