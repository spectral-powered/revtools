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

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.LookupSwitchInsnNode
import org.objectweb.asm.tree.TableSwitchInsnNode
import org.objectweb.asm.tree.TryCatchBlockNode
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceMethodVisitor
import java.io.PrintWriter
import java.io.StringWriter

private val PURE_OPCODES = setOf(
    -1,
    NOP,
    ACONST_NULL,
    ICONST_M1,
    ICONST_0,
    ICONST_1,
    ICONST_2,
    ICONST_3,
    ICONST_4,
    ICONST_5,
    LCONST_0,
    LCONST_1,
    FCONST_0,
    FCONST_1,
    FCONST_2,
    DCONST_0,
    DCONST_1,
    BIPUSH,
    SIPUSH,
    LDC,
    ILOAD,
    LLOAD,
    FLOAD,
    DLOAD,
    ALOAD,
    POP,
    POP2,
    DUP,
    DUP_X1,
    DUP_X2,
    DUP2,
    DUP2_X1,
    DUP2_X2,
    SWAP,
    IADD,
    LADD,
    FADD,
    DADD,
    ISUB,
    LSUB,
    FSUB,
    DSUB,
    IMUL,
    LMUL,
    FMUL,
    DMUL,
    /*
     * XXX(gpe): strictly speaking the *DEV and *REM instructions have side
     * effects (unless we can prove that the second argument is non-zero).
     * However, treating them as having side effects reduces the number of
     * dummy variables we can remove, so we pretend they don't have any side
     * effects.
     *
     * This doesn't seem to cause any problems with the client, as it doesn't
     * deliberately try to trigger divide-by-zero exceptions.
     */
    IDIV,
    LDIV,
    FDIV,
    DDIV,
    IREM,
    LREM,
    FREM,
    DREM,
    INEG,
    LNEG,
    FNEG,
    DNEG,
    ISHL,
    LSHL,
    ISHR,
    LSHR,
    IUSHR,
    LUSHR,
    IAND,
    LAND,
    IOR,
    LOR,
    IXOR,
    LXOR,
    I2L,
    I2F,
    I2D,
    L2I,
    L2F,
    L2D,
    F2I,
    F2L,
    F2D,
    D2I,
    D2L,
    D2F,
    I2B,
    I2C,
    I2S,
    LCMP,
    FCMPL,
    FCMPG,
    DCMPL,
    DCMPG,
    GETSTATIC,
    NEW,
    INSTANCEOF
)

private val IMPURE_OPCODES = setOf(
    IALOAD,
    LALOAD,
    FALOAD,
    DALOAD,
    AALOAD,
    BALOAD,
    CALOAD,
    SALOAD,
    ISTORE,
    LSTORE,
    FSTORE,
    DSTORE,
    ASTORE,
    IASTORE,
    LASTORE,
    FASTORE,
    DASTORE,
    AASTORE,
    BASTORE,
    CASTORE,
    SASTORE,
    IINC,
    IFEQ,
    IFNE,
    IFLT,
    IFGE,
    IFGT,
    IFLE,
    IF_ICMPEQ,
    IF_ICMPNE,
    IF_ICMPLT,
    IF_ICMPGE,
    IF_ICMPGT,
    IF_ICMPLE,
    IF_ACMPEQ,
    IF_ACMPNE,
    GOTO,
    JSR,
    RET,
    TABLESWITCH,
    LOOKUPSWITCH,
    IRETURN,
    LRETURN,
    FRETURN,
    DRETURN,
    ARETURN,
    RETURN,
    PUTSTATIC,
    GETFIELD,
    PUTFIELD,
    INVOKEVIRTUAL,
    INVOKESPECIAL,
    INVOKESTATIC,
    INVOKEINTERFACE,
    INVOKEDYNAMIC,
    NEWARRAY,
    ANEWARRAY,
    ARRAYLENGTH,
    ATHROW,
    CHECKCAST,
    MONITORENTER,
    MONITOREXIT,
    MULTIANEWARRAY,
    IFNULL,
    IFNONNULL
)

private val THROW_RETURN_OPCODES = listOf(
    IRETURN,
    LRETURN,
    FRETURN,
    DRETURN,
    ARETURN,
    RETURN,
    RET,
    ATHROW
)

val AbstractInsnNode.nextReal: AbstractInsnNode?
    get() {
        var insn = next
        while (insn != null && insn.opcode == -1) {
            insn = insn.next
        }
        return insn
    }

val AbstractInsnNode.previousReal: AbstractInsnNode?
    get() {
        var insn = previous
        while (insn != null && insn.opcode == -1) {
            insn = insn.previous
        }
        return insn
    }

val AbstractInsnNode.nextVirtual: AbstractInsnNode?
    get() {
        var insn = next
        while (insn != null && insn.opcode != -1) {
            insn = insn.next
        }
        return insn
    }

val AbstractInsnNode.previousVirtual: AbstractInsnNode?
    get() {
        var insn = previous
        while (insn != null && insn.opcode != -1) {
            insn = insn.previous
        }
        return insn
    }

val AbstractInsnNode.intConstant: Int?
    get() = when (this) {
        is IntInsnNode -> {
            if (opcode == BIPUSH || opcode == SIPUSH) {
                operand
            } else {
                null
            }
        }

        is LdcInsnNode -> {
            val cst = cst
            if (cst is Int) {
                cst
            } else {
                null
            }
        }

        else -> when (opcode) {
            ICONST_M1 -> -1
            ICONST_0 -> 0
            ICONST_1 -> 1
            ICONST_2 -> 2
            ICONST_3 -> 3
            ICONST_4 -> 4
            ICONST_5 -> 5
            else -> null
        }
    }

val AbstractInsnNode.isSequential: Boolean
    get() = when (this) {
        is LabelNode -> false
        is JumpInsnNode -> false
        is TableSwitchInsnNode -> false
        is LookupSwitchInsnNode -> false
        else -> opcode !in THROW_RETURN_OPCODES
    }

val AbstractInsnNode.isPure: Boolean
    get() = when (opcode) {
        in PURE_OPCODES -> true
        in IMPURE_OPCODES -> false
        else -> throw IllegalArgumentException()
    }

fun Int.toAbstractInsnNode(): AbstractInsnNode = when (this) {
    -1 -> InsnNode(ICONST_M1)
    0 -> InsnNode(ICONST_0)
    1 -> InsnNode(ICONST_1)
    2 -> InsnNode(ICONST_2)
    3 -> InsnNode(ICONST_3)
    4 -> InsnNode(ICONST_4)
    5 -> InsnNode(ICONST_5)
    in Byte.MIN_VALUE..Byte.MAX_VALUE -> IntInsnNode(BIPUSH, this)
    in Short.MIN_VALUE..Short.MAX_VALUE -> IntInsnNode(SIPUSH, this)
    else -> LdcInsnNode(this)
}

fun AbstractInsnNode.toPrettyString(): String {
    val printer = Textifier()

    val visitor = TraceMethodVisitor(printer)
    accept(visitor)

    StringWriter().use { stringWriter ->
        PrintWriter(stringWriter).use { printWriter ->
            printer.print(printWriter)
            return stringWriter.toString().trim()
        }
    }
}

fun AbstractInsnNode.toOpcodeString(): String = ""

fun TryCatchBlockNode.isBodyEmpty(): Boolean {
    var current = start.next

    while (true) {
        when {
            current == null -> error("Failed to reach end of try-catch block.")
            current === end -> return true
            current.opcode != -1 -> return false
            else -> current = current.next
        }
    }
}