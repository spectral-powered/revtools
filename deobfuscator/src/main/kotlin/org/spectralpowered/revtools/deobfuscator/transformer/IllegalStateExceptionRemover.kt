package org.spectralpowered.revtools.deobfuscator.transformer

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.spectralpowered.revtools.deobfuscator.Logger
import org.spectralpowered.revtools.deobfuscator.Transformer
import org.spectralpowered.revtools.deobfuscator.asm.InsnMatcher
import org.spectralpowered.revtools.deobfuscator.asm.tree.ClassGroup
import org.spectralpowered.revtools.deobfuscator.asm.tree.isStatic
import org.spectralpowered.revtools.deobfuscator.asm.tree.removeDeadCode
import java.lang.IllegalStateException

class IllegalStateExceptionRemover : Transformer {

    private var count = 0

    private val THROW_PATTERN = InsnMatcher.compile(
        """
                (ILOAD)
                ([ICONST_M1-LDC])
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
                ([ICONST_M1-LDC])
                ([IF_ICMPEQ-IF_ACMPNE])
                ([IRETURN-RETURN])
            """.trimIndent()
    )

    override fun run(group: ClassGroup) {
        for(cls in group.classes) {
            for(method in cls.methods) {
                for(match in THROW_PATTERN.match(method).filter { method.checkThrowPattern(it) }) {
                    method.removeCheck(match)
                    continue
                }

                for(match in RETURN_PATTERN.match(method).filter { method.checkReturnPattern(it) }) {
                    method.removeCheck(match)
                    continue
                }

                method.removeDeadCode()
            }
        }

        Logger.info("Removed $count IllegalStateException try-catch blocks.")
    }

    private fun MethodNode.checkThrowPattern(insns: List<AbstractInsnNode>): Boolean {
        val load = insns[0] as VarInsnNode
        val cst = insns[1]
        val new = insns[3] as TypeInsnNode
        if(load.`var` != lastArgIndex) return false
        if(cst.intConstant == null) return false
        return new.desc == "java/lang/IllegalStateException"
    }

    private fun MethodNode.checkReturnPattern(insns: List<AbstractInsnNode>): Boolean {
        val load = insns[0] as VarInsnNode
        val cst = insns[1]
        if(load.`var` != lastArgIndex) return false
        return cst.intConstant != null
    }

    private fun MethodNode.removeCheck(insns: List<AbstractInsnNode>) {
        val jump = insns[2] as JumpInsnNode
        val goto = JumpInsnNode(GOTO, jump.label)
        instructions.insert(insns.last(), goto)
        insns.forEach(instructions::remove)
        count++
    }

    private val MethodNode.lastArgIndex: Int get() {
        val offset = if(isStatic()) 1 else 0
        return (Type.getArgumentsAndReturnSizes(desc) shr 2) - offset - 1
    }

    private val AbstractInsnNode.intConstant: Int? get() = when(this) {
        is IntInsnNode -> when(opcode) {
            BIPUSH, SIPUSH -> operand
            else -> null
        }
        is LdcInsnNode -> when(cst) {
            is Int -> cst as Int
            else -> null
        }
        else -> when(opcode) {
            in ICONST_M1..ICONST_5 -> opcode - ICONST_0
            else -> null
        }
    }
}