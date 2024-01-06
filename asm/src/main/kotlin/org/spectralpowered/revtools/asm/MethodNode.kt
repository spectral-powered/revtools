package org.spectralpowered.revtools.asm

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.spectralpowered.revtools.asm.expr.ExprTree
import org.spectralpowered.revtools.asm.util.field

var MethodNode.cls: ClassNode by field()
val MethodNode.group get() = cls.group

val MethodNode.id get() = "${cls.id} $name $desc"

val MethodNode.exprTree get() = ExprTree.build(this)

fun MethodNode.isConstructor() = name == "<init>"
fun MethodNode.isInitializer() = name == "<clinit>"

fun MethodNode.isPrivate() = (access and ACC_PRIVATE) != 0
fun MethodNode.isStatic() = (access and ACC_STATIC) != 0
fun MethodNode.isAbstract() = (access and ACC_ABSTRACT) != 0