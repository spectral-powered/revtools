package org.spectralpowered.revtools.deobfuscator.client

import org.spectralpowered.revtools.deobfuscator.Deobfuscator
import java.io.File

object TestClientMain {

    @JvmStatic
    fun main(args: Array<String>) {
        val inputJar = File("build/deob/gamepack.jar")
        val outputJar = File("build/deob/gamepack.deob.jar")
        val deobfuscator = Deobfuscator(inputJar, outputJar)
        deobfuscator.run()
        TestClient(outputJar).start()
    }
}