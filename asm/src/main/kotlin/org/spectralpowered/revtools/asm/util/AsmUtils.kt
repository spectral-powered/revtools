package org.spectralpowered.revtools.asm.util

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.CheckClassAdapter
import org.spectralpowered.revtools.asm.group
import java.io.InputStream

fun readClassNode(input: InputStream, flags: Int = 0): ClassNode {
    val reader = ClassReader(input)
    val node = ClassNode()
    reader.accept(node, flags)
    return node
}

fun ByteArray.toClassNode(): ClassNode {
    val reader = ClassReader(this.inputStream())
    val node = ClassNode()
    reader.accept(node, 0)
    return node
}

fun ClassNode.toByteArray(flags: Int = ClassWriter.COMPUTE_FRAMES, check: Boolean = false): ByteArray {
    val writer = AsmClassWriter(group)
    val adapter = if(check) CheckClassAdapter(writer) else writer
    this.accept(adapter)
    return writer.toByteArray()
}

fun ClassNode.recomputeFrames(): ClassNode {
    val bytes = this.toByteArray()
    return bytes.toClassNode()
}