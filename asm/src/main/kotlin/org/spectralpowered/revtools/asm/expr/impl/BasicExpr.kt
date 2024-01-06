package org.spectralpowered.revtools.asm.expr.impl

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.spectralpowered.revtools.asm.expr.Expr
import org.spectralpowered.revtools.asm.expr.ExprTree
import org.spectralpowered.revtools.asm.toOpcodeString
import org.spectralpowered.revtools.asm.util.stackMetadata

open class BasicExpr(open val insn: AbstractInsnNode) : Expr {

    lateinit var tree: ExprTree internal set
    var index = -1
    var size = 0

    val opcode get() = insn.opcode
    val stackMetadata get() = insn.stackMetadata

    var parent: BasicExpr? = null
    val children = mutableListOf<BasicExpr>()

    var prev: BasicExpr? = null
    var next: BasicExpr? = null

    val instructions: List<AbstractInsnNode> get() {
        val insns = mutableListOf<AbstractInsnNode>()
        for(child in children.reversed()) {
            insns.addAll(child.instructions)
        }
        insns.add(insn)
        return insns
    }

    fun addChild(expr: BasicExpr) {
        expr.parent = this
        children.add(expr)
    }

    override fun toString(): String {
        val str = StringBuilder()
        str.append("${this::class.simpleName}[${insn.toOpcodeString()}]")
        if(children.isNotEmpty()) {
            str.append(" (")
            for(child in children) {
                str.append(child)
            }
            str.append(")")
        }
        return str.toString()
    }

    companion object {
        fun fromInsn(insn: AbstractInsnNode) = when(insn.opcode) {
            in GETSTATIC..PUTFIELD -> FieldExpr(insn as FieldInsnNode)
            in INVOKEVIRTUAL..INVOKEINTERFACE -> MethodExpr(insn as MethodInsnNode)
            LDC -> LdcExpr(insn as LdcInsnNode)
            in IADD..LXOR -> MathExpr(insn as InsnNode)
            in IFEQ..IFLE, IFNULL, IFNONNULL -> BranchExpr(insn as JumpInsnNode)
            in IF_ICMPEQ..IF_ACMPNE -> CmpBranchExpr(insn as JumpInsnNode)
            in ICONST_M1..SIPUSH -> ConstExpr(insn)
            else -> BasicExpr(insn)
        }
    }
}