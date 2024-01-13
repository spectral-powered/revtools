package org.spectralpowered.revtools.deobfuscator.asm

import org.objectweb.asm.ClassWriter
import org.spectralpowered.revtools.deobfuscator.asm.tree.ClassGroup
import org.spectralpowered.revtools.deobfuscator.asm.tree.isAssignableFrom
import org.spectralpowered.revtools.deobfuscator.asm.tree.isInterface
import org.spectralpowered.revtools.deobfuscator.asm.tree.superClass

class AsmClassWriter(private val group: ClassGroup, flags: Int) : ClassWriter(flags) {
    override fun getCommonSuperClass(type1: String, type2: String): String {
        var class1 = group.resolveClass(type1)
        val class2 = group.resolveClass(type2)
        return when {
            class1.isAssignableFrom(class2) -> type1
            class2.isAssignableFrom(class1) -> type2
            class1.isInterface() || class2.isInterface() -> "java/lang/Object"
            else -> {
                do {
                    class1 = class1.superClass!!
                } while(!class1.isAssignableFrom(class2))
                class1.name
            }
        }
    }
}