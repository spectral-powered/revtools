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

package org.spectralpowered.revtools

import org.objectweb.asm.Label
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.util.Printer

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
        insn = insn.next
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
        insn = insn.next
    }
    return insn
}

fun AbstractInsnNode.toOpcodeString(): String = when(this) {
    is LabelNode -> "LABEL"
    is LineNumberNode -> "LINE($line)"
    is FrameNode -> "FRAME"
    else -> Printer.OPCODES[opcode]
}