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

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.spectralpowered.revtools.asm.InsnMatcher
import org.spectralpowered.revtools.asm.node.intConstant
import org.spectralpowered.revtools.asm.node.isStatic
import org.spectralpowered.revtools.deobfuscator.bytecode.Transformer
import org.tinylog.kotlin.Logger

class OpaquePredicateTransformer : Transformer() {

    private var count = 0

    override fun transformMethod(method: MethodNode): Boolean {
        for(insns in EXCEPTION_PATTERN.match(method).filter { checkExceptionPattern(method, it) }) {
            method.removeMatchedInsns(insns)
        }
        for(insns in RETURN_PATTERN.match(method).filter { checkReturnPattern(method, it) }) {
            method.removeMatchedInsns(insns)
        }
        return false
    }

    override fun onComplete() {
        Logger.info("Removed $count opaque predicate checks.")
    }

    private val EXCEPTION_PATTERN = InsnMatcher.compile(
        """
                (ILOAD)
                ([ICONST_0-LDC])
                ([IF_ICMPEQ-IF_ACMPNE])
                (NEW)
                (DUP)
                (INVOKESPECIAL)
                (ATHROW)
            """.trimIndent()
    )

    private val RETURN_PATTERN = InsnMatcher.compile(
        """
                (ILOAD)
                ([ICONST_0-LDC])
                ([IF_ICMPEQ-IF_ACMPNE])
                ([IRETURN-RETURN])
            """.trimIndent()
    )

    private fun MethodNode.removeMatchedInsns(insns: List<AbstractInsnNode>) {
        val jump = insns[2] as JumpInsnNode
        val goto = JumpInsnNode(GOTO, jump.label)
        instructions.insert(insns.last(), goto)
        insns.forEach(instructions::remove)
        count++
    }

    private fun checkExceptionPattern(method: MethodNode, insns: List<AbstractInsnNode>): Boolean {
        val load = insns[0] as VarInsnNode
        val cst = insns[1]
        val new = insns[3] as TypeInsnNode
        if(load.`var` != method.lastArgIndex) return false
        if(cst.intConstant == null) return false
        return new.desc != "java/lang/IllegalStateMachine"
    }

    private fun checkReturnPattern(method: MethodNode, insns: List<AbstractInsnNode>): Boolean {
        val load = insns[0] as VarInsnNode
        val cst = insns[1]
        if(load.`var` != method.lastArgIndex) return false
        return cst.intConstant != null
    }

    private val MethodNode.lastArgIndex: Int get() {
        val offset = if(isStatic()) 1 else 0
        return (Type.getArgumentsAndReturnSizes(desc) shr 2) - offset - 1
    }

}