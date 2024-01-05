package org.spectralpowered.revtools.asm

import org.objectweb.asm.tree.ClassNode
import org.spectralpowered.revtools.asm.archive.AbstractArchive
import org.spectralpowered.revtools.asm.util.recomputeFrames

class ClassGroup {

    private val classMap = hashMapOf<String, ClassNode>()
    private val ignoredClassMap = hashMapOf<String, ClassNode>()
    val resources = hashMapOf<String, ByteArray>()

    val classes get() = classMap.values.toSet()
    val ignoredClasses get() = ignoredClassMap.values.toSet()

    fun addClass(cls: ClassNode) {
        cls.init(this)
        classMap[cls.name] = cls
    }

    fun removeClass(cls: ClassNode) {
        classMap.remove(cls.name)
    }

    fun addClasses(classes: Collection<ClassNode>) {
        for(cls in classes) {
            addClass(cls)
        }
    }

    fun replaceClass(old: ClassNode, new: ClassNode) {
        removeClass(old)
        addClass(new)
    }

    fun ignoreClass(cls: ClassNode) {
        ignoredClassMap[cls.name] = cls
        classMap.remove(cls.name)
    }

    fun ignoreClasses(predicate: (ClassNode) -> Boolean) {
        for(cls in classes) {
            if(predicate(cls)) ignoreClass(cls)
        }
    }

    fun getClass(name: String) = classMap[name]
    fun getIgnoredClass(name: String) = ignoredClassMap[name]

    fun findClass(name: String) = getClass(name) ?: getIgnoredClass(name)

    fun containsClass(name: String) = classMap.containsKey(name)

    fun readArchive(archive: AbstractArchive) {
        archive.read(this)
    }

    fun writeArchive(archive: AbstractArchive, writeIgnoredClasses: Boolean = true, writeResources: Boolean = true) {
        archive.write(this, writeIgnoredClasses, writeResources)
    }

    fun ignoreBouncyCastleClasses() {
        ignoreClasses { it.name.startsWith("org/bouncycastle") }
    }

    fun ignoreJsonClasses() {
        ignoreClasses { it.name.startsWith("org/json") }
    }

    fun init() {
        for(cls in classes.plus(ignoredClasses)) {
            cls.init(this)
            cls.recomputeFrames()
        }
    }
}