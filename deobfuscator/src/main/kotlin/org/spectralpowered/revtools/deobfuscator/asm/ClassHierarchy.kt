package org.spectralpowered.revtools.deobfuscator.asm

import org.spectralpowered.revtools.deobfuscator.asm.tree.ClassGroup
import java.util.ArrayDeque

class ClassHierarchy(private val group: ClassGroup) {

    private val tree = hashMapOf<String, ClassTree>()

    init {
        group.allClasses.forEach { cls ->
            val clsTree = tree.computeIfAbsent(cls.name) { ClassTree(cls.name) }
            if(cls.superName != null) {
                val superTree = tree.computeIfAbsent(cls.superName) { ClassTree(cls.superName) }
                clsTree.parents.add(superTree)
                superTree.children.add(clsTree)
            }
            if(cls.interfaces != null) {
                cls.interfaces.forEach { itf ->
                    val itfTree = tree.computeIfAbsent(itf) { ClassTree(itf) }
                    clsTree.parents.add(itfTree)
                    itfTree.children.add(clsTree)
                }
            }
        }
        tree.values.forEach { it.build() }
    }

    operator fun get(name: String) = tree[name]

    fun getAllParents(name: String) = tree[name]?.allParents?.map { group.resolveClass(it.name) } ?: emptyList()
    fun getAllChildren(name: String) = tree[name]?.allChildren?.map { group.resolveClass(it.name) } ?: emptyList()

    inner class ClassTree(val name: String) {

        val parents = hashSetOf<ClassTree>()
        val children = hashSetOf<ClassTree>()

        val allParents = hashSetOf<ClassTree>()
        val allChildren = hashSetOf<ClassTree>()

        internal fun build() {
            val queue = ArrayDeque<ClassTree>()

            queue.addAll(children)
            while(queue.isNotEmpty()) {
                val child = queue.removeFirst()
                allChildren.add(child)
                queue.addAll(child.children)
            }

            queue.addAll(parents)
            while(queue.isNotEmpty()) {
                val parent = queue.removeFirst()
                allParents.add(parent)
                queue.addAll(parent.parents)
            }
        }
    }
}