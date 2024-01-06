package org.spectralpowered.revtools.asm.expr.impl

import org.objectweb.asm.tree.InsnNode

class MathExpr(override val insn: InsnNode) : BasicExpr(insn) {

    var left: BasicExpr
        get() = children[0]
        set(value) { children[0] = value }

    var right: BasicExpr
        get() = children[1]
        set(value) { children[1] = value }

    val field get() = if(left is FieldExpr) left else if(right is FieldExpr) right else null
    val ldc get() = if(left is LdcExpr) left else if(right is LdcExpr) right else null
}