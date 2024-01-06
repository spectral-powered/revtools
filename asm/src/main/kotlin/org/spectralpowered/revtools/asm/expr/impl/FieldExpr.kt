package org.spectralpowered.revtools.asm.expr.impl

import org.objectweb.asm.Opcodes.GETFIELD
import org.objectweb.asm.Opcodes.GETSTATIC
import org.objectweb.asm.tree.FieldInsnNode
import org.spectralpowered.revtools.asm.findField
import org.spectralpowered.revtools.asm.group

class FieldExpr(override val insn: FieldInsnNode) : BasicExpr(insn) {

    var owner by insn::owner
    var name by insn::name
    var desc by insn::desc

    fun isGetter() = (opcode == GETFIELD || opcode == GETSTATIC)
    fun isSetter() = !isGetter()

    val field get() = tree.group.findClass(owner)?.findField(name, desc)

}