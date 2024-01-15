package org.spectralpowered.revtools.deobfuscator.asm.tree

import org.objectweb.asm.tree.AbstractInsnNode

val AbstractInsnNode.nextReal: AbstractInsnNode? get() {
    var insn = next
    while(insn != null && insn.opcode == -1) {
        insn = insn.next
    }
    return insn
}

val AbstractInsnNode.previousReal: AbstractInsnNode? get() {
    var insn = previous
    while(insn != null && insn.opcode == -1) {
        insn = insn.previous
    }
    return insn
}

val AbstractInsnNode.nextVirtual: AbstractInsnNode? get() {
    var insn = next
    while(insn != null && insn.opcode != -1) {
        insn = insn.next
    }
    return insn
}

val AbstractInsnNode.previousVirtual: AbstractInsnNode? get() {
    var insn = previous
    while(insn != null && insn.opcode != -1) {
        insn = insn.previous
    }
    return insn
}