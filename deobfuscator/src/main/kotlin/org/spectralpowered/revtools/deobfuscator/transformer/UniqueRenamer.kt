@file:Suppress("DuplicatedCode")

package org.spectralpowered.revtools.deobfuscator.transformer

import org.objectweb.asm.Type
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.spectralpowered.revtools.deobfuscator.Transformer
import org.spectralpowered.revtools.deobfuscator.annotation.ObfInfo
import org.spectralpowered.revtools.deobfuscator.asm.ClassHierarchy
import org.spectralpowered.revtools.deobfuscator.asm.tree.*
import kotlin.reflect.jvm.jvmName

class UniqueRenamer : Transformer {

    private val mappings = hashMapOf<String, String>()

    private var classCount = 0
    private var methodCount = 0
    private var fieldCount = 0

    override fun run(group: ClassGroup) {
        generateMappings(group)
        applyMappings(group)
    }

    private fun generateMappings(group: ClassGroup) {
        val hierarchy = ClassHierarchy(group)

        for(cls in group.classes) {
            if(!cls.name.isObfuscatedName()) continue
            val newName = "class${++classCount}"
            cls.addObfInfoAnnotation()
            mappings[cls.key] = newName
        }

        for(cls in group.classes) {
            for(method in cls.methods) {
                if(mappings.containsKey(method.key)) continue
                if(!method.name.isObfuscatedName()) continue
                val newName = "method${++methodCount}"
                method.addObfInfoAnnotation()
                mappings[method.key] = newName
                for (childCls in hierarchy.getAllChildren(method.cls.name)) {
                    val key = "${childCls.key}.${method.name}${method.desc}"
                    childCls.getMethod(method.name, method.desc)?.addObfInfoAnnotation()
                    mappings[key] = newName
                }
            }
        }

        for(cls in group.classes) {
            for(field in cls.fields) {
                if(mappings.containsKey(field.key)) continue
                if(!field.name.isObfuscatedName()) continue
                val newName = "field${++fieldCount}"
                field.addObfInfoAnnotation()
                mappings[field.key] = newName
                for(childCls in hierarchy.getAllChildren(field.cls.name)) {
                    val key = "${childCls.key}.${field.name}"
                    childCls.getField(field.name, field.desc)?.addObfInfoAnnotation()
                    mappings[key] = newName
                }
            }
        }
    }

    private fun applyMappings(group: ClassGroup) {
        val remapper = SimpleRemapper(mappings)
        val classMap = hashMapOf<ClassNode, ClassNode>()
        group.classes.forEach { cls ->
            val newCls = ClassNode()
            cls.accept(ClassRemapper(newCls, remapper))
            classMap[cls] = newCls
        }

        classMap.forEach { (old, new) ->
            group.replaceClass(old, new)
        }
        group.build()
    }

    private fun ClassNode.addObfInfoAnnotation() {
        val annotation = AnnotationNode(Type.getObjectType(ObfInfo::class.qualifiedName!!.replace(".", "/")).descriptor)
        annotation.values = listOf(
            "name", name
        )
        visibleAnnotations = visibleAnnotations ?: mutableListOf()
        visibleAnnotations.add(annotation)
    }

    private fun MethodNode.addObfInfoAnnotation() {
        val annotation = AnnotationNode(Type.getObjectType(ObfInfo::class.qualifiedName!!.replace(".", "/")).descriptor)
        annotation.values = listOf(
            "name", name,
            "desc", desc
        )
        visibleAnnotations = visibleAnnotations ?: mutableListOf()
        visibleAnnotations.add(annotation)
    }

    private fun FieldNode.addObfInfoAnnotation() {
        val annotation = AnnotationNode(Type.getObjectType(ObfInfo::class.qualifiedName!!.replace(".", "/")).descriptor)
        annotation.values = listOf(
            "name", name,
            "desc", desc
        )
        visibleAnnotations = visibleAnnotations ?: mutableListOf()
        visibleAnnotations.add(annotation)
    }

    private fun String.isObfuscatedName(): Boolean {
        return (this.length <= 2) || (this.length == 3 && this !in listOf("add", "get", "put", "set", "run", "<init>", "<clinit>"))
    }
}