package org.spectralpowered.revtools.asm.archive

import org.objectweb.asm.tree.ClassNode
import org.spectralpowered.revtools.asm.ClassGroup
import org.spectralpowered.revtools.asm.jarIndex
import org.spectralpowered.revtools.asm.util.readClassNode
import org.spectralpowered.revtools.asm.util.recomputeFrames
import org.spectralpowered.revtools.asm.util.toByteArray
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

class JarArchive(file: File) : AbstractArchive(file) {

    constructor(path: String) : this(File(path))

    override fun read(group: ClassGroup) {
        JarFile(file).use { jar ->
            var index = 0
            for(entry in jar.entries().asSequence()) {
                if(entry.name.endsWith(".class")) {
                    val input = jar.getInputStream(entry)
                    val cls = readClassNode(input)
                    group.addClass(cls)
                    cls.jarIndex = index
                } else if(!entry.isDirectory) {
                    val name = entry.name
                    val bytes = jar.getInputStream(entry).readBytes()
                    group.resources[name] = bytes
                }
                index++
            }
        }

        for(cls in group.classes) {
            cls.recomputeFrames()
        }
    }

    override fun write(group: ClassGroup, writeIgnoredClasses: Boolean, writeResources: Boolean) {
        if(file.exists()) {
            file.deleteRecursively()
        } else if(file.parentFile != null && !file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }

        val classes = mutableListOf<ClassNode>()
        classes.addAll(group.classes)
        if(writeIgnoredClasses) classes.addAll(group.ignoredClasses)
        classes.sortBy { it.jarIndex }

        JarOutputStream(file.outputStream()).use { output ->
            for(cls in classes) {
                output.putNextEntry(JarEntry("${cls.name}.class"))
                output.write(cls.toByteArray(check = true))
                output.closeEntry()
            }
            if(writeResources) {
                for((name, bytes) in group.resources) {
                    output.putNextEntry(JarEntry(name))
                    output.write(bytes)
                    output.closeEntry()
                }
            }
        }
    }
}