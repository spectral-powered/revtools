package org.spectralpowered.revtools.asm

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.spectralpowered.revtools.asm.archive.JarArchive
import org.spectralpowered.revtools.asm.util.toClassNode
import kotlin.streams.toList

class ClassGroupTest : FunSpec({

    val group = ClassGroup()

    beforeSpec {
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

    test("Class Group contains classes") {
        group.classes.size shouldBeGreaterThan 0
    }

    test("Write classes to Jar") {
        val startCount = group.classes.size
        val archive = JarArchive("test.jar")
        group.writeArchive(archive)
        archive.file.exists() shouldBe true

        val outGroup = ClassGroup()
        outGroup.readArchive(archive)
        val outCount = outGroup.classes.size
        startCount shouldBeEqual outCount

        archive.file.deleteRecursively()
    }
})