package org.spectralpowered.revtools.deobfuscator.transformer

import com.google.common.collect.MultimapBuilder
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.Type.*
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.spectralpowered.revtools.deobfuscator.Logger
import org.spectralpowered.revtools.deobfuscator.Transformer
import org.spectralpowered.revtools.deobfuscator.annotation.ObfInfo
import org.spectralpowered.revtools.deobfuscator.asm.tree.*
import org.spectralpowered.revtools.deobfuscator.util.isDeobfuscatedName
import kotlin.math.max

class UnusedArgRemover : Transformer {

    private var count = 0

    private val rootMethods = hashSetOf<String>()
    private val unusedArgMethodsMap = MultimapBuilder.hashKeys().arrayListValues().build<String, MethodNode>()
    private val unusedArgMethods = unusedArgMethodsMap.asMap()
    private val methodOpaques = hashMapOf<String, Int>()

    override fun run(group: ClassGroup) {


        for(cls in group.classes) {
            val supers = group.findSupers(cls)
            for(method in cls.methods) {
                if(supers.none { it.getMethod(method.name, method.desc) != null }) {
                    rootMethods.add(method.key)
                }
            }
        }

        for(cls in group.classes) {
            for(method in cls.methods) {
                val key = group.findMethodOverride(method.cls.name, method.name, method.desc, rootMethods) ?: continue
                unusedArgMethodsMap.put(key, method)
            }
        }

        val itr = unusedArgMethods.iterator()
        for((_, method) in itr) {
            if(method.any { !it.canRemoveLastArg() }) itr.remove()
        }

        for(cls in group.classes) {
            for(method in cls.methods) {
                for(insn in method.instructions) {
                    if(insn !is MethodInsnNode) continue
                    val key = group.findMethodOverride(insn.owner, insn.name, insn.desc, unusedArgMethods.keys) ?: continue
                    if(insn.previous.intConstant == null) {
                        unusedArgMethods.remove(key)
                    }
                }
            }
        }

        unusedArgMethodsMap.values().forEach { method ->
            val oldDesc = method.desc
            val newDesc = oldDesc.dropLastArg()
            method.desc = newDesc
            count++
        }

        for(method in group.classes.flatMap { it.methods }) {
            val insns = method.instructions
            for(insn in insns) {
                if(insn !is MethodInsnNode) continue
                val key = group.findMethodOverride(insn.owner, insn.name, insn.desc, unusedArgMethods.keys) ?: continue
                insn.desc = insn.desc.dropLastArg()
                val prev = insn.previous
                unusedArgMethods[key]?.forEach { methodOpaques[it.key] = prev.intConstant!! }
                insns.remove(prev)
            }
        }

        val methodsMap = group.allClasses.flatMap { it.methods }.associateBy { it.key }
        methodOpaques.forEach { (key, opaque) ->
            val method = methodsMap[key] ?: return@forEach
            val annotation = method.visibleAnnotations.firstOrNull { it.desc == Type.getDescriptor(ObfInfo::class.java) } ?: return@forEach
            annotation.values.addAll(listOf(
                "opaque", opaque
            ))
        }

        Logger.info("Removed unused arguments from $count methods.")
    }

    private fun ClassGroup.findSupers(cls: ClassNode): Collection<ClassNode> {
        return listOfNotNull(cls.superName).plus(cls.interfaces).mapNotNull { getClass(it) }.flatMap { this.findSupers(it).plus(it) }
    }

    private fun ClassGroup.findMethodOverride(owner: String, name: String, desc: String, methods: Collection<String>): String? {
        val key = "$owner.$name$desc"
        if(key in methods) return key
        if(name.startsWith("<init>")) return null
        val cls = getClass(owner) ?: return null
        for(superCls in findSupers(cls)) {
            return findMethodOverride(superCls.name, name, desc, methods) ?: continue
        }
        return null
    }

    private val MethodNode.lastArgIndex: Int get() {
        val offset = if(isStatic()) 1 else 0
        return (Type.getArgumentsAndReturnSizes(desc) shr 2) - offset - 1
    }

    private fun MethodNode.canRemoveLastArg(): Boolean {
        val args = Type.getArgumentTypes(desc)
        if(args.isEmpty()) return false
        val lastArg = args.last()
        if(lastArg !in arrayOf(BYTE_TYPE, SHORT_TYPE, INT_TYPE)) return false
        if(isAbstract()) return true
        for(insn in instructions) {
            if(insn !is VarInsnNode) continue
            if(insn.`var` == lastArgIndex) return false
        }
        return name.isDeobfuscatedName()
    }

    private fun String.dropLastArg(): String {
        val type = Type.getMethodType(this)
        return Type.getMethodDescriptor(type.returnType, *type.argumentTypes.copyOf(max(0, type.argumentTypes.size - 1)))
    }

    private val AbstractInsnNode.intConstant: Int? get() = when(this) {
        is IntInsnNode -> when(opcode) {
            BIPUSH, SIPUSH -> operand
            else -> null
        }
        is LdcInsnNode -> when(cst) {
            is Int -> cst as Int
            else -> null
        }
        else -> when(opcode) {
            in ICONST_M1..ICONST_5 -> opcode - ICONST_0
            else -> null
        }
    }

}