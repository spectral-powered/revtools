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

package org.spectralpowered.revtools.deobfuscator.bytecode.transformer

import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.spectralpowered.revtools.asm.InsnMatcher
import org.spectralpowered.revtools.asm.MemberRef
import org.spectralpowered.revtools.deobfuscator.bytecode.Transformer
import org.tinylog.kotlin.Logger

class CounterTransformer : Transformer() {

    private val counters = mutableSetOf<MemberRef>()

    override fun preTransform(): Boolean {
        val references = mutableMapOf<MemberRef, Int>()
        val resets = mutableMapOf<MemberRef, Int>()
        val increments = mutableMapOf<MemberRef, Int>()

        for(cls in pool.classes) {
            for(method in cls.methods) {
                if(method.instructions.size() > 0) {
                    reduceCounters(method, references, resets, increments)
                }
            }
        }
        deleteCounters(references, resets, increments)
        return false
    }

    override fun transformMethod(method: MethodNode): Boolean {
        for(match in RESET_PATTERN.match(method)) {
            val putstatic = match[1] as FieldInsnNode
            if(MemberRef(putstatic) in counters) {
                match.forEach(method.instructions::remove)
            }
        }

        for(match in INCREMENT_PATTERN.match(method)) {
            val getstatic = MemberRef(match[0] as FieldInsnNode)
            val putstatic = MemberRef(match[3] as FieldInsnNode)
            if(getstatic == putstatic && putstatic in counters) {
                match.forEach(method.instructions::remove)
            }
        }

        return false
    }

    override fun onComplete() {
        Logger.info("Removed ${counters.size} counter / incrementing / decrementing instructions.")
    }

    private fun reduceCounters(
        method: MethodNode,
        references: MutableMap<MemberRef, Int>,
        resets: MutableMap<MemberRef, Int>,
        increments: MutableMap<MemberRef, Int>
    ) {
        for(insn in method.instructions) {
            if(insn is MethodInsnNode) {
                references.merge(MemberRef(insn), 1, Integer::sum)
            }
        }

        for(match in RESET_PATTERN.match(method)) {
            val putstatic = MemberRef(match[1] as FieldInsnNode)
            resets.merge(putstatic, 1, Integer::sum)
        }

        for(match in INCREMENT_PATTERN.match(method)) {
            val getstatic = MemberRef(match[0] as FieldInsnNode)
            val putstatic = MemberRef(match[3] as FieldInsnNode)
            if(getstatic == putstatic) {
                increments.merge(putstatic, 1, Integer::sum)
            }
        }
    }

    private fun deleteCounters(
        references: MutableMap<MemberRef, Int>,
        resets: MutableMap<MemberRef, Int>,
        increments: MutableMap<MemberRef, Int>
    ) {
        for((counter, value) in references) {
            if(resets[counter] != 1) {
                continue
            }

            val counterIinc = increments[counter] ?: 0
            if(value != counterIinc * 2 + 1) {
                continue
            }

            val owner = pool.findClass(counter.owner)!!
            owner.fields.removeIf { it.name == counter.name && it.desc == counter.desc }
            counters.add(counter)
        }
    }

    private companion object {
        private val RESET_PATTERN = InsnMatcher.compile("ICONST_0 PUTSTATIC")
        private val INCREMENT_PATTERN = InsnMatcher.compile("GETSTATIC ICONST_1 IADD PUTSTATIC")
    }
}