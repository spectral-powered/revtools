package org.spectralpowered.revtools.asm.util

import org.objectweb.asm.ClassWriter
import org.spectralpowered.revtools.asm.ClassGroup

class AsmClassLoader(private val group: ClassGroup) : ClassLoader() {

    private val String.asmName get() = this.replace(".", "/")
    private val String.javaName get() = this.replace("/", ".")

    private fun getClassBytes(name: String): ByteArray? {
        val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val cls = group.findClass(name.asmName) ?: return null
        cls.accept(writer)
        return writer.toByteArray()
    }

    override fun loadClass(name: String): Class<*> {
        val bytes = getClassBytes(name)
        return if(bytes != null) {
            super.defineClass(name.javaName, bytes, 0, bytes.size)
        } else {
            super.loadClass(name.javaName)
        }
    }
}