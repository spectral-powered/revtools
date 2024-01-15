package org.spectralpowered.revtools.deobfuscator.transformer

import org.objectweb.asm.Opcodes.GOTO
import org.objectweb.asm.tree.JumpInsnNode
import org.spectralpowered.revtools.deobfuscator.Logger
import org.spectralpowered.revtools.deobfuscator.Transformer
import org.spectralpowered.revtools.deobfuscator.asm.tree.ClassGroup
import org.spectralpowered.revtools.deobfuscator.asm.tree.nextReal
import org.spectralpowered.revtools.deobfuscator.asm.tree.removeDeadCode

class RedundantGotoRemover : Transformer {

    private var count = 0

    override fun run(group: ClassGroup) {
        for(cls in group.classes) {
            for(method in cls.methods) {
                method.removeDeadCode()
                for(insn in method.instructions) {
                    if(insn.opcode == GOTO) {
                        insn as JumpInsnNode
                        if(insn.nextReal === insn.label.nextReal) {
                            method.instructions.remove(insn)
                            count++
                        }
                    }
                }
            }
        }

        Logger.info("Removed $count redundant GOTO instructions.")
    }
}