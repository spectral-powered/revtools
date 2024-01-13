package org.spectralpowered.revtools.deobfuscator.transformer

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.spectralpowered.revtools.deobfuscator.Logger
import org.spectralpowered.revtools.deobfuscator.Transformer
import org.spectralpowered.revtools.deobfuscator.annotation.ObfInfo
import org.spectralpowered.revtools.deobfuscator.asm.tree.ClassGroup
import org.spectralpowered.revtools.deobfuscator.asm.tree.init

class AddDeobClasses : Transformer {

    private val classesToAdd = listOf(
        ObfInfo::class.java
    )

    override fun run(group: ClassGroup) {
        classesToAdd.forEach { klass ->
            val cls = ClassNode()
            val bytes = ClassLoader.getSystemResourceAsStream("${Type.getInternalName(klass).replace(".", "/")}.class")?.readBytes()
                ?: error("Could not find class: '${Type.getInternalName(klass).replace(".", "/")}.class'.")
            val reader = ClassReader(bytes)
            reader.accept(cls, ClassReader.SKIP_FRAMES)
            group.addClass(cls)
        }

        Logger.info("Added ${classesToAdd.size} deobfuscator classes.")
    }
}