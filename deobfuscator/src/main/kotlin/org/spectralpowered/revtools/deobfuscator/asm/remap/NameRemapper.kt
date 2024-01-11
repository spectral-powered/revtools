package org.spectralpowered.revtools.deobfuscator.asm.remap

import org.objectweb.asm.Handle
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InnerClassNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode
import org.objectweb.asm.tree.TryCatchBlockNode
import org.objectweb.asm.tree.TypeInsnNode
import org.spectralpowered.revtools.deobfuscator.asm.tree.*

class NameRemapper {

    val mappings = hashMapOf<String, String>()

    fun mapClass(cls: ClassNode, newName: String) {
        if(mappings.containsKey(cls.key)) return
        mappings[cls.key] = newName
    }

    fun mapMethod(method: MethodNode, newName: String) {
        if(mappings.containsKey(method.key)) return
        mappings[method.key] = newName
        method.cls.parents.plus(method.cls.children).mapNotNull { it.getMethod(method.name, method.desc) }.forEach { m ->
            mapMethod(m, newName)
        }
    }

    fun mapField(field: FieldNode, newName: String) {
        if(mappings.containsKey(field.key)) return
        mappings[field.key] = newName
        field.cls.parents.plus(field.cls.children).mapNotNull { it.getField(field.name, field.desc) }.forEach { f ->
            mapField(f, newName)
        }
    }

    fun remap(group: ClassGroup) {
        val remapper = SimpleRemapper(mappings)
        for(cls in group.classes) {
            cls.remap(remapper)
        }
    }

    private fun ClassNode.remap(remapper: Remapper) {
        val origName = name
        name = remapper.mapType(origName)
        signature = remapper.mapSignature(signature, false)
        superName = remapper.mapType(superName)
        interfaces = interfaces?.map(remapper::mapType)

        val origOuterClass = outerClass
        outerClass = remapper.mapType(origOuterClass)

        if(outerMethod != null) {
            outerMethod = remapper.mapMethodName(origOuterClass, outerMethod, outerMethodDesc)
            outerMethodDesc = remapper.mapMethodDesc(outerMethodDesc)
        }

        innerClasses.forEach { innerClass ->
            innerClass.remap(remapper)
        }

        methods.forEach { method ->
            method.remap(origName, remapper)
        }

        fields.forEach { field ->
            field.remap(origName, remapper)
        }
    }

    private fun InnerClassNode.remap(remapper: Remapper) {
        name = remapper.mapType(name)
        outerName = remapper.mapType(outerName)
        innerName = remapper.mapType(innerName)
    }

    private fun FieldNode.remap(origClassName: String, remapper: Remapper) {
        name = remapper.mapFieldName(origClassName, name, desc)
        desc = remapper.mapDesc(desc)
        signature = remapper.mapSignature(signature, true)
        value = remapper.mapValue(value)
    }

    private fun MethodNode.remap(origClassName: String, remapper: Remapper) {
        name = remapper.mapMethodName(origClassName, name, desc)
        desc = remapper.mapMethodDesc(desc)
        signature = remapper.mapSignature(signature, false)
        exceptions = exceptions.map(remapper::mapType)

        for(insn in instructions) {
            insn.remap(remapper)
        }

        for(tcb in tryCatchBlocks) {
            tcb.remap(remapper)
        }
    }

    private fun TryCatchBlockNode.remap(remapper: Remapper) {
        type = remapper.mapType(type)
    }

    private fun AbstractInsnNode.remap(remapper: Remapper) {
        when(this) {
            is FieldInsnNode -> {
                val origOwner = owner
                owner = remapper.mapType(origOwner)
                name = remapper.mapMethodName(origOwner, name, desc)
                desc = remapper.mapMethodDesc(desc)
            }
            is MethodInsnNode -> {
                val origOwner = owner
                owner = remapper.mapType(origOwner)
                name = remapper.mapFieldName(origOwner, name, desc)
                desc = remapper.mapDesc(desc)
            }
            is InvokeDynamicInsnNode -> {
                name = remapper.mapInvokeDynamicMethodName(name, desc)
                desc = remapper.mapMethodDesc(desc)
                bsm = remapper.mapValue(bsm) as Handle
                bsmArgs = bsmArgs.map { remapper.mapValue(it) }.toTypedArray()
            }
            is TypeInsnNode -> desc = remapper.mapType(desc)
            is LdcInsnNode -> cst = remapper.mapValue(cst)
            is MultiANewArrayInsnNode -> desc = remapper.mapType(desc)
        }
    }
}