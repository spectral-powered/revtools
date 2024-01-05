package org.spectralpowered.revtools.asm

import org.objectweb.asm.Opcodes.ACC_ABSTRACT
import org.objectweb.asm.Opcodes.ACC_INTERFACE
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.spectralpowered.revtools.asm.util.field

fun ClassNode.init(group: ClassGroup) {
    this.group = group
    for(method in methods) method.cls = this
    for(field in fields) field.cls = this
}

var ClassNode.group: ClassGroup by field()
var ClassNode.jarIndex: Int by field()

val ClassNode.id get() = name

val ClassNode.constructors get() = methods.filter { it.isConstructor() }
val ClassNode.initializers get() = methods.filter { it.isInitializer() }

val ClassNode.memberMethods get() = methods.filter { !it.isStatic() }
val ClassNode.staticMethods get() = methods.filter { it.isStatic() }

val ClassNode.memberFields get() = fields.filter { !it.isStatic() }
val ClassNode.staticFields get() = fields.filter { it.isStatic() }

fun ClassNode.isAbstract() = (access and ACC_ABSTRACT) != 0
fun ClassNode.isInterface() = (access and ACC_INTERFACE) != 0

fun ClassNode.getMethod(name: String, desc: String) = methods.firstOrNull { it.name == name && it.desc == desc }
fun ClassNode.getField(name: String, desc: String) = fields.firstOrNull { it.name == name && it.desc == desc }

fun ClassNode.findMethod(name: String, desc: String): MethodNode? {
    val method = getMethod(name, desc)
    if(method != null) return method
    return group.findClass(superName)?.findMethod(name, desc)
}

fun ClassNode.findField(name: String, desc: String): FieldNode? {
    val field = getField(name, desc)
    if(field != null) return field
    return group.findClass(superName)?.findField(name, desc)
}