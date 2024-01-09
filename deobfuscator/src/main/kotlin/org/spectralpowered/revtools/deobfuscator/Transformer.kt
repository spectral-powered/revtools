package org.spectralpowered.revtools.deobfuscator

import org.spectralpowered.revtools.asm.ClassGroup

interface Transformer {

    fun run(group: ClassGroup)

}