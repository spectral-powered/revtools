package org.spectralpowered.revtools.deobfuscator

import org.spectralpowered.revtools.deobfuscator.asm.tree.ClassGroup

interface Transformer {

    fun run(group: ClassGroup)

}