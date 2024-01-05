package org.spectralpowered.revtools.asm.util

import org.objectweb.asm.ClassWriter
import org.spectralpowered.revtools.asm.ClassGroup

class AsmClassWriter(group: ClassGroup, flags: Int = COMPUTE_FRAMES) : ClassWriter(flags) {

    private val loader = AsmClassLoader(group)

    private fun readClass(name: String) = try {
        Class.forName(name.replace("/", "."), false, loader)
    } catch (e: Throwable) {
        throw ClassNotFoundException(e.toString())
    }

    override fun getCommonSuperClass(type1: String, type2: String): String = try {
        var klass1 = readClass(type1)
        val klass2 = readClass(type2)

        when {
            klass1.isAssignableFrom(klass2) -> type1
            klass2.isAssignableFrom(klass1) -> type2
            klass1.isInterface || klass2.isInterface -> "java/lang/Object"
            else -> {
                do {
                    klass1 = klass1.superclass
                } while(!klass1.isAssignableFrom(klass2))
                klass1.name.replace(".", "/")
            }
        }
    } catch (e: Throwable) {
        "java/lang/Object"
    }
}