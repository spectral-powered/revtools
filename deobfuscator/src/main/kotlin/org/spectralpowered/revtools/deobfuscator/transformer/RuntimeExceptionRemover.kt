package org.spectralpowered.revtools.deobfuscator.transformer

import org.objectweb.asm.tree.TryCatchBlockNode
import org.spectralpowered.revtools.asm.ClassGroup
import org.spectralpowered.revtools.deobfuscator.Transformer

class RuntimeExceptionRemover : Transformer {

    private var count = 0

    override fun run(group: ClassGroup) {
        for(cls in group.classes) {
            for(method in cls.methods) {
                val toRemove = mutableListOf<TryCatchBlockNode>()
                for(tcb in method.tryCatchBlocks) {
                    if(tcb.type == "java/lang/RuntimeException") {
                        toRemove.add(tcb)
                    }
                }
                method.tryCatchBlocks.removeAll(toRemove)
                count += toRemove.size
            }
        }

        println("Removed $count RuntimeException try-catch blocks")
    }
}