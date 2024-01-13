package org.spectralpowered.revtools.deobfuscator.transformer

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.VarInsnNode
import org.spectralpowered.revtools.deobfuscator.Logger
import org.spectralpowered.revtools.deobfuscator.Transformer
import org.spectralpowered.revtools.deobfuscator.asm.InsnMatcher
import org.spectralpowered.revtools.deobfuscator.asm.tree.ClassGroup
import org.spectralpowered.revtools.deobfuscator.asm.tree.isStatic
import org.spectralpowered.revtools.deobfuscator.asm.tree.removeDeadCode

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
                listOf(THROW_PATTERN, RETURN_PATTERN).forEach { pattern ->
                    for(match in pattern.match(method).filter { method.checkPattern(it) }) {
                        val jump = match[2] as JumpInsnNode
                        val goto = JumpInsnNode(GOTO, jump.label)
                        method.instructions.insert(match.last(), goto)
                        match.forEach { method.instructions.remove(it) }
                        count++
                    }
                }
                method.removeDeadCode()
            }
        }

        Logger.info("Removed $count IllegalStateException try-catch blocks.")
    }

    private fun MethodNode.checkPattern(insns: List<AbstractInsnNode>): Boolean {
        val load = insns[0] as VarInsnNode
        val cst = insns[1]
        if(load.`var` != lastArgIndex) return false
        if(cst.opcode !in ICONST_M1..ICONST_5 && cst.opcode !in BIPUSH..SIPUSH || cst.opcode == LDC && (cst as LdcInsnNode).cst !is Int) return false
        if(insns[3] is TypeInsnNode) {
            val new = insns[3] as TypeInsnNode
            if(new.desc != "java/lang/IllegalStateException") return false
        }
        return true
    }

    private val MethodNode.lastArgIndex: Int get() {
        val offset = if(isStatic()) 1 else 0
        return (Type.getArgumentsAndReturnSizes(desc) shr 2) - offset - 1
    }
}