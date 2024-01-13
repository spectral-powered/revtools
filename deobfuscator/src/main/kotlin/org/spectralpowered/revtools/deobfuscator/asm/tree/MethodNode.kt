package org.spectralpowered.revtools.deobfuscator.asm.tree

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TryCatchBlockNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.spectralpowered.revtools.deobfuscator.util.field

fun MethodNode.init(cls: ClassNode) {
    this.cls = cls
}

fun MethodNode.build() {

}

var MethodNode.cls: ClassNode by field()
val MethodNode.group get() = cls.group

val MethodNode.id get() = "${cls.id} $name $desc"
val MethodNode.key get() = "${cls.key}.$name$desc"

fun MethodNode.isConstructor() = name == "<init>"
fun MethodNode.isInitializer() = name == "<clinit>"

fun MethodNode.isPrivate() = (access and ACC_PRIVATE) != 0
fun MethodNode.isAbstract() = (access and ACC_ABSTRACT) != 0
fun MethodNode.isStatic() = (access and ACC_STATIC) != 0

fun MethodNode.removeDeadCode() {
    fun TryCatchBlockNode.isBodyEmpty(): Boolean {
        var cur = start.next
        while(true) {
            when {
                cur == null -> error("Failed to reach end of try-catch block.")
                cur === end -> return true
                cur.opcode != -1 -> return false
                else -> cur = cur.next
            }
        }
    }

    var changed: Boolean
    do {
        changed = false
        val analyzer = Analyzer(BasicInterpreter())
        val frames = analyzer.analyze(cls.name, this)

        val insns = instructions.iterator()
        var i = 0
        for(insn in insns) {
            if(frames[i++] != null || insn is LabelNode) {
                continue
            }
            insns.remove()
            changed = true
        }
        changed = changed or tryCatchBlocks.removeIf { it.isBodyEmpty() }
    } while(changed)
}