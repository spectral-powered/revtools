package org.spectralpowered.revtools.deobfuscator.transformer

import org.objectweb.asm.Opcodes.ATHROW
import org.spectralpowered.revtools.deobfuscator.Logger
import org.spectralpowered.revtools.deobfuscator.Transformer
import org.spectralpowered.revtools.deobfuscator.asm.tree.ClassGroup
import org.spectralpowered.revtools.deobfuscator.asm.tree.nextReal

class UnusedTryCatchRemover : Transformer {

    private var count = 0

    override fun run(group: ClassGroup) {
        for(cls in group.classes) {
            for(method in cls.methods) {
                for(insn in method.instructions) {
                    if(insn.opcode != ATHROW) {
                        continue
                    }

                    val removeTryCatch = method.tryCatchBlocks.removeIf { tcb ->
                        tcb.handler.nextReal === insn
                    }

                    if(removeTryCatch) {
                        method.instructions.remove(insn)
                        count++
                    }
                }
            }
        }

        Logger.info("Removed $count unused try-catch blocks.")
    }
}