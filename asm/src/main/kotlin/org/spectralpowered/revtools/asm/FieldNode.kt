package org.spectralpowered.revtools.asm

import org.objectweb.asm.Opcodes.ACC_PRIVATE
import org.objectweb.asm.Opcodes.ACC_STATIC
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.spectralpowered.revtools.asm.util.field

var FieldNode.cls: ClassNode by field()
val FieldNode.group get() = cls.group

fun FieldNode.isPrivate() = (access and ACC_PRIVATE) != 0
fun FieldNode.isStatic() = (access and ACC_STATIC) != 0