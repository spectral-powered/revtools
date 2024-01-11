package org.spectralpowered.revtools.deobfuscator.asm.tree

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.spectralpowered.revtools.deobfuscator.util.field

fun FieldNode.init(cls: ClassNode) {
    this.cls = cls
}

fun FieldNode.build() {

}

var FieldNode.cls: ClassNode by field()
val FieldNode.group get() = cls.group

val FieldNode.id get() = "${cls.id} $name"
val FieldNode.key get() = "${cls.key}.$name"

fun FieldNode.isPrivate() = (access and ACC_PRIVATE) != 0
fun FieldNode.isFinal() = (access and ACC_FINAL) != 0
fun FieldNode.isStatic() = (access and ACC_STATIC) != 0