@file:Suppress("UNCHECKED_CAST")

package org.spectralpowered.revtools.deobfuscator.util

import com.google.common.reflect.ClassPath
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FunSpec
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.spectralpowered.revtools.deobfuscator.asm.tree.ClassGroup
import org.spectralpowered.revtools.deobfuscator.asm.tree.fromBytes
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.stream.Collectors

abstract class AsmTestSpec(body: AsmTestSpec.() -> Unit = {}) : FunSpec(body as FunSpec.() -> Unit), BeforeSpecListener {

    val group = ClassGroup()
    var packageName: String = "classes"

    fun packageName(value: String) {
        this.packageName = value
    }

    init {
        body()
    }

    override suspend fun beforeSpec(spec: Spec) {
        group.clear()
        val classes = findClassesByPackageName(packageName)
        classes.forEach { group.addClass(it) }
        group.build()
    }

    private fun findClassesByPackageName(packageName: String): Set<ClassNode> {
        return ClassPath.from(ClassLoader.getSystemClassLoader())
            .allClasses
            .filter { it.packageName.startsWith(packageName) }
            .map { it.resourceName }
            .map { klass ->
                val bytes = ClassLoader.getSystemClassLoader().getResourceAsStream("/$klass")!!.readBytes()
                val cls = ClassNode()
                cls
            }
            .toSet()
    }


}