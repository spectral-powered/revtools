package org.spectralpowered.revtools.asm

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.LookupSwitchInsnNode
import org.objectweb.asm.tree.TableSwitchInsnNode
import org.objectweb.asm.util.Printer
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceMethodVisitor
import java.io.PrintWriter
import java.io.StringWriter

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

fun AbstractInsnNode.isSequential() = when(this) {
    is LabelNode -> false
    is JumpInsnNode -> false
    is TableSwitchInsnNode -> false
    is LookupSwitchInsnNode -> false
    else -> opcode !in THROW_RETURN_OPCODES
}

fun AbstractInsnNode.toPrettyString(): String {
    val printer = Textifier()
    val visitor = TraceMethodVisitor(printer)
    accept(visitor)
    StringWriter().use { writer ->
        PrintWriter(writer).use {
            printer.print(it)
            return writer.toString().trim()
        }
    }
}

fun AbstractInsnNode.toOpcodeString(): String {
    return when(this) {
        is LabelNode -> "LABEL ${this.toPrettyString()}"
        is LineNumberNode -> "LINENUMBER ${this.line}"
        is FrameNode -> this.toPrettyString()
        else -> Printer.OPCODES[opcode]
    }
}

val AbstractInsnNode.intConstant: Int? get() = when(this) {
    is IntInsnNode -> when(opcode) {
        BIPUSH, SIPUSH -> operand
        else -> null
    }
    is LdcInsnNode -> when(cst) {
        is Int -> cst as Int
        else -> null
    }
    else -> when(opcode) {
        in ICONST_M1..ICONST_5 -> opcode - 3
        else -> null
    }
}

