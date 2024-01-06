package org.spectralpowered.revtools.asm.expr.impl

import org.objectweb.asm.tree.LdcInsnNode

class LdcExpr(override val insn: LdcInsnNode) : BasicExpr(insn) {

    var cst by insn::cst

    fun isNumber() = cst is Number

    val number get() = if(!isNumber()) null else cst as Number

}