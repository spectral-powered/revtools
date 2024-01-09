package org.spectralpowered.revtools.deobfuscator.transformer

import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.spectralpowered.revtools.asm.ClassGroup
import org.spectralpowered.revtools.asm.cls
import org.spectralpowered.revtools.deobfuscator.Transformer

class DeadCodeRemover : Transformer {

    private var count = 0

    override fun run(group: ClassGroup) {
        group.classes.forEach { cls ->
            cls.methods.forEach { method ->
                val insns = method.instructions.toArray()
                val frames = Analyzer(BasicInterpreter()).analyze(method.cls.name, method)
                for(i in frames.indices) {
                    if(frames[i] == null) {
                        method.instructions.remove(insns[i])
                        count++
                    }
                }
            }
        }

        println("Removed $count dead instructions.")
    }
}