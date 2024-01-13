package org.spectralpowered.revtools.deobfuscator.transformer

import org.spectralpowered.revtools.deobfuscator.Logger
import org.spectralpowered.revtools.deobfuscator.Transformer
import org.spectralpowered.revtools.deobfuscator.asm.tree.ClassGroup
import org.spectralpowered.revtools.deobfuscator.asm.tree.removeDeadCode

class RuntimeExceptionRemover : Transformer {

    private var count = 0

    override fun run(group: ClassGroup) {
        for(cls in group.classes) {
            for(method in cls.methods) {
                val itr = method.tryCatchBlocks.iterator()
                while(itr.hasNext()) {
                    val tcb = itr.next()
                    if(tcb.type == "java/lang/RuntimeException") {
                        itr.remove()
                        count++
                    }
                }
                method.removeDeadCode()
            }
        }

        Logger.info("Removed $count RuntimeException try-catch blocks.")
    }
}