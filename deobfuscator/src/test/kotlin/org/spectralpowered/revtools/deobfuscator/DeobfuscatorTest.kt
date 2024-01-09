package org.spectralpowered.revtools.deobfuscator

import java.io.File

object DeobfuscatorTest {

    @JvmStatic
    fun main(args: Array<String>) {
        val inputJar = File("build/deob/gamepack.jar")
        val outputJar = File("build/deob/gamepack.deob.jar")
        val deobfuscator = Deobfuscator(inputJar, outputJar)
        deobfuscator.run()
        TestClient(outputJar).start()
    }
}