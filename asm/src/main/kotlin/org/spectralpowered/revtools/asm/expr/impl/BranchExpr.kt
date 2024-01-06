package org.spectralpowered.revtools.asm.expr.impl

import org.objectweb.asm.tree.JumpInsnNode

open class BranchExpr(override val insn: JumpInsnNode) : BasicExpr(insn) {

}