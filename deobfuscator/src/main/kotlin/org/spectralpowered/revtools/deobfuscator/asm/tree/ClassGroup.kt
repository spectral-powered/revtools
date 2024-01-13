package org.spectralpowered.revtools.deobfuscator.asm.tree

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ACC_SUPER
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayDeque
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import kotlin.io.path.readBytes

class ClassGroup {

    private val classMap = hashMapOf<String, ClassNode>()
    private val ignoredClassMap = hashMapOf<String, ClassNode>()
    private val runtimeClassMap = hashMapOf<String, ClassNode>()
    val resources = hashMapOf<String, ByteArray>()

    val classes get() = classMap.values.toSet()
    val ignoredClasses get() = ignoredClassMap.values.toSet()
    val runtimeClasses get() = runtimeClassMap.values.toSet()
    val allClasses get() = classes.plus(ignoredClasses).plus(runtimeClasses).toSet()

    fun addClass(cls: ClassNode) {
        if(classMap.containsKey(cls.name)) error("Class already in group.")
        cls.init(this)
        classMap[cls.name] = cls
    }

    fun removeClass(cls: ClassNode) {
        if(!classMap.containsKey(cls.name)) error("Class not found in group.")
        classMap.remove(cls.name)
    }

    fun replaceClass(old: ClassNode, new: ClassNode) {
        removeClass(old)
        addClass(new)
    }

    fun ignoreClass(cls: ClassNode) {
        if(ignoredClassMap.containsKey(cls.name)) error("Class already ignored.")
        if(!classMap.containsKey(cls.name)) error("Class not found in group.")
        ignoredClassMap[cls.name] = cls
        classMap.remove(cls.name)
    }

    fun ignoreClassIf(predicate: (ClassNode) -> Boolean) {
        val toIgnore = mutableListOf<ClassNode>()
        classes.forEach { cls ->
            if(predicate(cls)) toIgnore.add(cls)
        }
        toIgnore.forEach { ignoreClass(it) }
    }

    fun addRuntimeClass(cls: ClassNode) {
        if(runtimeClassMap.containsKey(cls.name)) error("Runtime class already in group.")
        cls.init(this)
        runtimeClassMap[cls.name] = cls
    }

    fun getClass(name: String) = classMap[name]
    fun getIgnoredClass(name: String) = ignoredClassMap[name]
    fun getRuntimeClass(name: String) = runtimeClassMap[name]
    fun findClass(name: String) = getClass(name) ?: getIgnoredClass(name) ?: getRuntimeClass(name)

    fun resolveClass(name: String): ClassNode {
        var ret = findClass(name)
        if(ret != null) return ret

        /*
         * Load runtime class from system classLoader
         */
        var path: Path? = null
        val url = ClassLoader.getSystemResource("$name.class")
        if(url != null) {
            val uri = url.toURI()
            path = Paths.get(uri)
            if(uri.scheme == "jrt" && !Files.exists(path)) {
                path = Paths.get(URI(uri.scheme, uri.userInfo, uri.host, uri.port, "/modules${uri.path}", uri.query, uri.fragment))
            }
        }

        return if(path != null) {
            ret = ClassNode().fromBytes(path.readBytes(), ClassReader.SKIP_CODE)
            addRuntimeClass(ret)
            ret
        } else {
            ret = ClassNode()
            ret.access = ACC_PUBLIC or ACC_SUPER
            ret.name = name
            ret.superName = if(name == "java/lang/Object") null else "java/lang/Object"
            ret.interfaces = listOf()
            ret.init(this)
            addRuntimeClass(ret)
            ret
        }
    }

    fun clear() {
        classMap.clear()
        ignoredClassMap.clear()
        runtimeClassMap.clear()
    }

    /**
     * Builds extra info able to be derived from the class group.
     * !NOTE! Should only be called after all classes have been added to this group.
     */
    fun build() {
        allClasses.forEach { it.build() }
    }

    fun readJar(file: File) {
        JarFile(file).use { jar ->
            var index = 0
            jar.entries().asSequence().forEach { entry ->
                if(entry.name.endsWith(".class")) {
                    val cls = ClassNode().fromInputStream(jar.getInputStream(entry), ClassReader.SKIP_FRAMES)
                    cls.jarIndex = index++
                    addClass(cls)
                } else {
                    resources[entry.name] = jar.getInputStream(entry).readBytes()
                }
            }
        }
    }

    fun writeJar(file: File, includeIgnored: Boolean = true, includeResources: Boolean = true) {
        if(file.exists()) file.deleteRecursively()
        else if(file.parentFile != null) file.parentFile.mkdirs()
        JarOutputStream(file.outputStream()).use { output ->
            val toWrite = mutableListOf<ClassNode>()
            toWrite.addAll(classes)
            if(includeIgnored) toWrite.addAll(ignoredClasses)
            toWrite.sortBy { it.jarIndex }
            toWrite.forEach { cls ->
                output.putNextEntry(JarEntry("${cls.name}.class"))
                output.write(cls.toBytes(ClassWriter.COMPUTE_MAXS))
                output.closeEntry()
            }
            if(includeResources) {
                resources.forEach { (name, bytes) ->
                    output.putNextEntry(JarEntry(name))
                    output.write(bytes)
                    output.closeEntry()
                }
            }
        }
    }

    companion object {
        fun fromJar(file: File) = ClassGroup().also {
            it.readJar(file)
            it.build()
        }
    }
}