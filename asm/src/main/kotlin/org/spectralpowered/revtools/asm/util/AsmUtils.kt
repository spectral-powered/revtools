package org.spectralpowered.revtools.asm.util

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.JSRInlinerAdapter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.util.CheckClassAdapter
import java.io.InputStream

fun readClassNode(input: InputStream, flags: Int = 0): ClassNode {
    val reader = ClassReader(input)
    val node = ClassNode()
    reader.accept(node, flags)
    return node
}

fun ByteArray.toClassNode(flags: Int = 0): ClassNode {
    return readClassNode(this.inputStream(), flags)
}

fun ClassNode.recomputeFrames(loader: ClassLoader): ClassNode {
    val bytes = this.toByteArray(loader)
    return bytes.toClassNode()
}

fun ClassNode.toByteArray(
    loader: ClassLoader,
    flags: Int = ClassWriter.COMPUTE_FRAMES,
    checkClass: Boolean = false
): ByteArray {
    this.inlineJsrs()
    val writer = AsmClassWriter(loader, flags)
    val adapter = when {
        checkClass -> CheckClassAdapter(writer)
        else -> writer
    }
    this.accept(adapter)
    return writer.toByteArray()
}

val ClassNode.hasFrameInfo: Boolean get() {
    var hasInfo = false
    for(method in methods) {
        hasInfo = hasInfo || method.instructions.any { it is FrameNode }
    }
    return hasInfo
}

fun ClassNode.inlineJsrs() {
    this.methods = methods.map { it.jsrInlined }
}

val MethodNode.jsrInlined: MethodNode get() {
    val adapter = JSRInlinerAdapter(null, access, name, desc, signature, exceptions?.toTypedArray())
    this.accept(adapter)
    return LabelFilterer(adapter).build()
}

class AsmClassWriter(private val loader: ClassLoader, flags: Int) : ClassWriter(flags) {

    private fun readClass(name: String) = try {
        Class.forName(name.replace("/", "."), false, loader)
    } catch (e: Throwable) {
        throw ClassNotFoundException(e.toString())
    }

    override fun getCommonSuperClass(name1: String, name2: String): String = try {
        var cls1 = readClass(name1)
        val cls2 = readClass(name2)

        when {
            cls1.isAssignableFrom(cls2) -> name1
            cls2.isAssignableFrom(cls1) -> name2
            cls1.isInterface || cls2.isInterface -> "java/lang/Object"
            else -> {
                do {
                    cls1 = cls1.superclass
                } while(!cls1.isAssignableFrom(cls2))
                cls1.name.replace(".", "/")
            }
        }
    } catch (e: Throwable) {
        "java/lang/Object"
    }
}