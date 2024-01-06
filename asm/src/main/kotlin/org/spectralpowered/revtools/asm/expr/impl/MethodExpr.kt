package org.spectralpowered.revtools.asm.expr.impl

import org.objectweb.asm.tree.MethodInsnNode
import org.spectralpowered.revtools.asm.findMethod

class MethodExpr(override val insn: MethodInsnNode) : BasicExpr(insn) {

    var owner by insn::owner
    var name by insn::name
    var desc by insn::desc
    var itf by insn::itf

    val method get() = tree.group.findClass(owner)?.findMethod(name, desc)

}