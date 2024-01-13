package org.spectralpowered.revtools.deobfuscator.transformer

import com.google.common.collect.MultimapBuilder
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.spectralpowered.revtools.deobfuscator.Logger
import org.spectralpowered.revtools.deobfuscator.Transformer
import org.spectralpowered.revtools.deobfuscator.asm.tree.*
import org.spectralpowered.revtools.deobfuscator.util.isDeobfuscatedName

class UnusedMethodRemover : Transformer {

    private var count = 0

    private val superClasses = MultimapBuilder.hashKeys().arrayListValues().build<ClassNode, String>()
    private val subClasses = MultimapBuilder.hashKeys().arrayListValues().build<ClassNode, String>()
    private val usedMethods = mutableSetOf<String>()

    override fun run(group: ClassGroup) {
        for(cls in group.classes) {
            superClasses.put(cls, cls.superName)
            superClasses.putAll(cls, cls.interfaces)
        }

        superClasses.forEach { cls, superClass ->
            group.getClass(superClass)?.also { subClasses.put(it, cls.name) }
        }

        group.classes.flatMap { it.methods }
            .flatMap { it.instructions.asSequence() }
            .mapNotNull { it as? MethodInsnNode }
            .map { "${it.owner}.${it.name}${it.desc}" }
            .toSet()
            .apply {
                usedMethods.addAll(this)
            }

        for(cls in group.classes) {
            val methods = cls.methods.iterator()
            while(methods.hasNext()) {
                val method = methods.next()
                if(method.isUsed()) continue
                methods.remove()
                count++
            }
        }

        Logger.info("Removed $count unused methods.")
    }

    private fun MethodNode.isUsed(): Boolean {
        if(isConstructor() || isInitializer()) return true
        if(!name.isDeobfuscatedName()) return true
        if(key in usedMethods) return true

        var supers = superClasses[cls]
        while(supers.isNotEmpty()) {
            for(cls in supers) {
                if(isJvmMethod(cls, name, desc)) return true
                if("$cls.$name$desc" in usedMethods) return true
            }
            supers = supers.mapNotNull { group.getClass(it) }.flatMap { superClasses[it] }
        }

        var subs = subClasses[cls]
        while(subs.isNotEmpty()) {
            for(cls in subs) {
                if("$cls.$name$desc" in usedMethods) return true
            }
            subs = subs.flatMap { subClasses[group.getClass(it)] }
        }

        return false
    }

    private fun isJvmMethod(owner: String, name: String, desc: String): Boolean {
        try {
            var classes = listOf(Class.forName(Type.getObjectType(owner).className))
            while(classes.isNotEmpty()) {
                for(cls in classes) {
                    if(cls.declaredMethods.any { it.name == name && Type.getMethodDescriptor(it) == desc }) {
                        return true
                    }
                }
                classes = classes.flatMap { listOfNotNull(it.superclass).plus(it.interfaces) }
            }
        } catch (e: Exception) { /* Ignored Handler */ }
        return false
    }
}