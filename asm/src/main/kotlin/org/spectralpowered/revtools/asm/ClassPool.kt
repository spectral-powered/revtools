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

package org.spectralpowered.revtools.asm

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes.ACC_STATIC
import org.objectweb.asm.tree.ClassNode
import org.spectralpowered.revtools.asm.node.*
import org.spectralpowered.revtools.asm.remap.AsmRemapper
import org.spectralpowered.revtools.asm.remap.ClassPoolMapper
import org.spectralpowered.revtools.asm.remap.remap
import org.spectralpowered.revtools.asm.util.DisjointSet
import org.spectralpowered.revtools.asm.util.ForestDisjointSet
import org.spectralpowered.revtools.asm.util.toClassName
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

class ClassPool {

    private val classMap: SortedMap<String, ClassNode> = TreeMap()

    val classes get() = classMap.values.filterNot { it.isIgnored || it.isRuntime }
    val ignoredClasses get() = classMap.values.filter { it.isIgnored }
    val runtimeClasses get() = classMap.values.filter { it.isRuntime }
    val allClasses get() = classMap.values

    fun containsClass(name: String) = classMap.containsKey(name)

    fun addClass(cls: ClassNode) = classMap.put(cls.name, cls.also { it.init(this) })
    fun removeClass(name: String) = classMap.remove(name)

    fun replaceClass(old: ClassNode, new: ClassNode) {
        removeClass(old.name)
        addClass(new)
    }

    fun renameClass(cls: ClassNode, oldName: String) {
        removeClass(oldName)
        addClass(cls)
    }

    fun getClass(name: String) = classMap[name]

    fun findClass(name: String): ClassNode? = computeIfAbsent(name) {
        val ret = getClass(name)
        if(ret != null) return@computeIfAbsent ret

        val clazz = try {
            ClassLoader.getSystemClassLoader().loadClass(name.toClassName())
        } catch (e: ClassNotFoundException) {
            return@computeIfAbsent null
        }

        return@computeIfAbsent RuntimeClassNode(clazz).also {
            addClass(it)
            it.build()
        }
    }

    fun ignoreClass(name: String) {
        for(cls in allClasses) {
            if(cls.name == name) {
                cls.isIgnored = true
            }
        }
    }

    fun ignoreClasses(predicate: (ClassNode) -> Boolean) {
        for(cls in allClasses) {
            if(predicate(cls)) {
                cls.isIgnored = true
            }
        }
    }

    fun loadJar(file: File) {
        JarFile(file).use { jar ->
            for((index, entry) in jar.entries().asSequence().withIndex()) {
                if(!entry.name.endsWith(".class")) continue
                val bytes = jar.getInputStream(entry).readBytes()
                val cls = ClassNode().fromBytes(bytes, ClassReader.SKIP_FRAMES)
                addClass(cls)
                cls.jarIndex = index
            }
        }
    }

    fun writeJar(file: File, includeIgnored: Boolean = false) {
        Files.deleteIfExists(file.toPath())
        if(file.parentFile?.exists() != true) file.parentFile?.mkdirs()
        file.createNewFile()
        JarOutputStream(file.outputStream()).use { jos ->
            for(cls in allClasses) {
                if(cls.isRuntime || (cls.isIgnored && !includeIgnored)) continue
                jos.putNextEntry(JarEntry("${cls.name}.class"))
                jos.write(cls.toBytes())
                jos.closeEntry()
            }
        }
    }

    fun build(postLogic: (ClassPool) -> Unit = {}) {
        for(cls in classes) cls.reset()
        for(cls in classes) cls.build()
        postLogic(this)
    }

    fun remap(remapper: AsmRemapper) {
        ClassPoolMapper(this, remapper).remap()
    }

    private inline fun computeIfAbsent(name: String, block: (String) -> ClassNode?): ClassNode? {
        if(classMap.containsKey(name)) return classMap[name]
        val cls = block(name)?.also { addClass(it) }
        return cls
    }

    fun getMethodMemberRef(memberRef: MemberRef) = findClass(memberRef.owner)?.getMethod(memberRef.name, memberRef.desc)
    fun getFieldMemberRef(memberRef: MemberRef) = findClass(memberRef.owner)?.getField(memberRef.name, memberRef.desc)

    fun createInheritedMethodSets() = createInheritedMemberSets(
            ClassNode::memberMethods,
            ClassNode::getMethodAccess,
            fields = false
    )

    fun createInheritedFieldSets() = createInheritedMemberSets(
            ClassNode::memberFields,
            ClassNode::getFieldAccess,
            fields = true
    )

    private fun createInheritedMemberSets(
            getMembers: (ClassNode) -> List<MemberDesc>,
            getMemberAccess: (ClassNode, MemberDesc) -> Int?,
            fields: Boolean
    ): DisjointSet<MemberRef> {
        val disjointSet = ForestDisjointSet<MemberRef>()
        val classMembers = mutableMapOf<ClassNode, Set<MemberDesc>>()
        for(cls in classes) {
            computeInheritedMembers(getMembers, getMemberAccess, fields, classMembers, disjointSet, cls)
        }
        return disjointSet
    }

    private fun computeInheritedMembers(
            getMembers: (ClassNode) -> List<MemberDesc>,
            getMemberAccess: (ClassNode, MemberDesc) -> Int?,
            fields: Boolean,
            classMembers: MutableMap<ClassNode, Set<MemberDesc>>,
            disjointSet: DisjointSet<MemberRef>,
            cls: ClassNode
    ): Set<MemberDesc> {
        classMembers[cls]?.also { return it }
        val results = mutableSetOf<MemberDesc>()
        for(superCls in listOfNotNull(cls.superClass).plus(cls.interfaceClasses)) {
            val members = computeInheritedMembers(getMembers, getMemberAccess, fields, classMembers, disjointSet, superCls)
            for(member in members) {
                val access = getMemberAccess(cls, member)
                if(access != null && (access and ACC_STATIC != 0 || member.name == "<init>" || fields)) {
                    continue
                }
                val partition1 = disjointSet.add(MemberRef(cls.name, member))
                val partition2 = disjointSet.add(MemberRef(superCls.name, member))
                disjointSet.union(partition1, partition2)
                results.add(member)
            }
        }
        for(member in getMembers(cls)) {
            disjointSet.add(MemberRef(cls.name, member))
            results.add(member)
        }
        classMembers[cls] = results
        return results
    }
}