@file:Suppress("DuplicatedCode")

package org.spectralpowered.revtools.deobfuscator.asm.tree

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.ACC_ABSTRACT
import org.objectweb.asm.Opcodes.ACC_INTERFACE
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.util.CheckClassAdapter
import org.spectralpowered.revtools.deobfuscator.util.field
import org.spectralpowered.revtools.deobfuscator.util.nullField
import java.io.InputStream

fun ClassNode.init(group: ClassGroup) {
    this.group = group
    methods.forEach { it.init(this) }
    fields.forEach { it.init(this) }
}

fun ClassNode.build() {
    // Reset
    superClass = null
    interfaceClasses.clear()
    subClasses.clear()
    implementerClasses.clear()

    // Build
    superClass = superName?.let { group.resolveClass(it) }
    superClass?.subClasses?.add(this)
    interfaces.map { group.resolveClass(it) }.forEach { itf ->
        interfaceClasses.add(itf)
        itf.implementerClasses.add(this)
    }

    methods.forEach { it.build() }
    fields.forEach { it.build() }
}

var ClassNode.group: ClassGroup by field()
var ClassNode.jarIndex: Int by field { -1 }

var ClassNode.superClass: ClassNode? by nullField()
val ClassNode.interfaceClasses: MutableSet<ClassNode> by field { mutableSetOf() }
val ClassNode.subClasses: MutableSet<ClassNode> by field { mutableSetOf() }
val ClassNode.implementerClasses: MutableSet<ClassNode> by field { mutableSetOf() }

val ClassNode.parents: Collection<ClassNode> get() = listOfNotNull(superClass).plus(interfaceClasses)
val ClassNode.children: Collection<ClassNode> get() = subClasses.plus(implementerClasses)

val ClassNode.id get() = name
val ClassNode.key get() = name

val ClassNode.memberMethods get() = methods.filter { !it.isStatic() }
val ClassNode.memberFields get() = fields.filter { !it.isStatic() }
val ClassNode.staticMethods get() = methods.filter { it.isStatic() }
val ClassNode.staticFields get() = fields.filter { it.isStatic() }

val ClassNode.constructors get() = methods.filter { it.isConstructor() }
val ClassNode.initializers get() = methods.filter { it.isInitializer() }

fun ClassNode.isConcrete() = group.getClass(name) != null
fun ClassNode.isIgnored() = group.getIgnoredClass(name) != null
fun ClassNode.isRuntime() = group.getRuntimeClass(name) != null

fun ClassNode.isAbstract() = (access and ACC_ABSTRACT) != 0
fun ClassNode.isInterface() = (access and ACC_INTERFACE) != 0

fun ClassNode.getMethod(name: String, desc: String) = methods.firstOrNull { it.name == name && it.desc == desc }
fun ClassNode.getField(name: String, desc: String = "") = fields.firstOrNull { it.name == name && (desc.isBlank() || it.desc == desc) }

fun ClassNode.getMethodByKey(key: String) = methods.firstOrNull { it.key == key }
fun ClassNode.getFieldByKey(key: String) = fields.firstOrNull { it.key == key }

fun ClassNode.findMethod(name: String, desc: String): MethodNode? {
    var ret = getMethod(name, desc)
    if(ret != null) return ret
    var parents = this.parents
    do {
        ret = parents.firstNotNullOfOrNull { it.getMethod(name, desc) }
        parents = parents.flatMap { it.parents }
    } while(ret == null && parents.isNotEmpty())
    return ret ?: this.parents.map { it.getMethod(name, desc) }.firstOrNull()
}

fun ClassNode.findField(name: String, desc: String = ""): FieldNode? {
    var ret = getField(name, desc)
    if(ret != null) return ret

    ret = superClass?.findField(name, desc)
    if(ret != null) return ret

    return null
}

fun ClassNode.fromInputStream(input: InputStream, flags: Int): ClassNode {
    val reader = ClassReader(input)
    reader.accept(this, flags)
    return this
}

fun ClassNode.fromBytes(bytes: ByteArray, flags: Int) = fromInputStream(bytes.inputStream(), flags)

fun ClassNode.toBytes(flags: Int): ByteArray {
    val writer = ClassWriter(flags)
    val adapter = CheckClassAdapter(writer, true)
    this.accept(adapter)
    return writer.toByteArray()
}

