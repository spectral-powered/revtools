package org.spectralpowered.revtools.asm

import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import org.spectralpowered.revtools.asm.util.toClassNode
import kotlin.streams.toList

object AsmTest : BeforeSpecListener {

    val group = ClassGroup()

    override suspend fun beforeSpec(spec: Spec) {
        val pkgName = "testclasses"
        val input = ClassLoader.getSystemClassLoader().getResourceAsStream(pkgName.replace(Regex("[.]"), "/"))!!
        val reader = input.bufferedReader()
        val classes = reader.lines()
            .filter { it.endsWith(".class") }
            .map { try {
                Class.forName("$pkgName.${it.substring(0, it.lastIndexOf('.'))}")
            } catch (e: ClassNotFoundException) { null } }
            .toList()
            .filterNotNull()
            .toSet()
        for(klass in classes) {
            val bytes = ClassGroupTest::class.java.getResourceAsStream("/${pkgName.replace(".", "/")}/${klass.simpleName}.class")!!.readBytes()
            val cls = bytes.toClassNode()
            group.addClass(cls)
        }
        group.init()
    }
}