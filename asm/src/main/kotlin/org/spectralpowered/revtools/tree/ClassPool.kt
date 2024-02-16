/*
 * Copyright (C) 2024 Spectral Powered <https://github.com/spectral-powered>
 * @author Kyle Escobar <https://github.com/kyle-escobar>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.spectralpowered.revtools.tree

import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

class ClassPool {

    private val resourceMap = mutableMapOf<String, ByteArray>()
    private val classMap = mutableMapOf<String, ClassNode>()
    private val ignoredClassMap = mutableMapOf<String, ClassNode>()
    private val runtimeClassMap = mutableMapOf<String, ClassNode>()

    val classes get() = classMap.values.toSet()
    val ignoredClasses get() = ignoredClassMap.values.toSet()
    val runtimeClasses get() = runtimeClassMap.values.toSet()
    val allClasses get() = classes.plus(ignoredClasses).plus(runtimeClasses)

    fun addClass(cls: ClassNode) {
        val map = when(cls.classType) {
            ClassType.RESOLVED -> classMap
            ClassType.IGNORED -> ignoredClassMap
            ClassType.RUNTIME -> runtimeClassMap
        }
        cls.pool = this
        map[cls.key] = cls
    }

    fun removeClass(cls: ClassNode) {
        val map = when(cls.classType) {
            ClassType.RESOLVED -> classMap
            ClassType.IGNORED -> ignoredClassMap
            ClassType.RUNTIME -> runtimeClassMap
        }
        map.remove(cls.key)
    }

    fun ignoreClass(cls: ClassNode) {
        removeClass(cls)
        cls.classType = ClassType.IGNORED
        addClass(cls)
    }

    fun unignoreClass(cls: ClassNode) {
        removeClass(cls)
        cls.classType = ClassType.RESOLVED
        addClass(cls)
    }

    fun getClass(name: String) = classMap[name] ?: ignoredClassMap[name] ?: runtimeClassMap[name]

    fun findClass(name: String): ClassNode? {
        val ret = getClass(name)
        if(ret != null) return ret
        return try {
            val input = ClassLoader.getPlatformClassLoader().getResourceAsStream("$name.class")!!
            val rtret = ClassNode().fromInputStream(input)
            rtret.classType = ClassType.RUNTIME
            addClass(rtret)
            rtret
        } catch (e: Exception) {
            null
        }
    }

    fun addResource(name: String, bytes: ByteArray) {
        resourceMap[name] = bytes
    }

    fun readJar(file: File) {
        JarFile(file).use { jar ->
            jar.entries().asSequence().forEach { entry ->
                val bytes = jar.getInputStream(entry).readAllBytes()
                val name = entry.name
                if(name.endsWith(".class")) {
                    val cls = ClassNode().fromBytes(bytes)
                    cls.classType = ClassType.RESOLVED
                    addClass(cls)
                } else {
                    resourceMap[name] = bytes
                }
            }
        }
    }

    fun writeJar(file: File, includeIgnored: Boolean = true, includeResources: Boolean = false) {
        if(file.exists()) file.deleteRecursively()
        if(file.parentFile?.exists() == false) file.parentFile.mkdirs()
        file.createNewFile()
        JarOutputStream(file.outputStream()).use { jos ->
            val writeClasses = mutableListOf<ClassNode>()
            writeClasses.addAll(classes)
            if(includeIgnored) writeClasses.addAll(ignoredClasses)
            writeClasses.sortBy { it.jarIndex }
            for(cls in writeClasses) {
                jos.putNextEntry(JarEntry("${cls.name}.class"))
                jos.write(cls.toBytes())
                jos.closeEntry()
            }
            for((resName, resBytes) in resourceMap) {
                jos.putNextEntry(JarEntry(resName))
                jos.write(resBytes)
                jos.closeEntry()
            }
        }
    }

    fun init() {
        for(cls in allClasses) cls.reset()
        for(cls in allClasses) cls.init()
    }
}