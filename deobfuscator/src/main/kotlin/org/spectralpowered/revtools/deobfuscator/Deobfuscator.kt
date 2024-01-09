package org.spectralpowered.revtools.deobfuscator

import org.spectralpowered.revtools.asm.ClassGroup
import org.spectralpowered.revtools.asm.archive.JarArchive
import org.spectralpowered.revtools.deobfuscator.transformer.DeadCodeRemover
import org.spectralpowered.revtools.deobfuscator.transformer.RuntimeExceptionRemover
import java.io.File
import kotlin.reflect.full.createInstance

class Deobfuscator(private val inputJar: File, private val outputJar: File) {

    private val group = ClassGroup()
    private val transformers = mutableListOf<Transformer>()

    private fun registerTransformers() {
        /**
         * === Deobfuscator Transformers ===
         */
        register<RuntimeExceptionRemover>()
        register<DeadCodeRemover>()

        /**
         * === End ===
         */
        println("Registered ${transformers.size} transformers.")
    }

    private fun loadInputJar() {
        println("Initializing.")
        val inputArchive = JarArchive(inputJar)
        group.readArchive(inputArchive)
        group.ignoreBouncyCastleClasses()
        group.ignoreJsonClasses()
        group.init()
        println("Loaded ${group.classes.size} classes from input jar.")
    }

    private fun saveOutputJar() {
        println("Saving ${group.classes.size} classes to output jar.")
        val outputArchive = JarArchive(outputJar)
        group.writeArchive(outputArchive, writeIgnoredClasses = true, writeResources = false)
    }

    fun run() {
        println("Preparing Deobfuscator.")

        registerTransformers()
        loadInputJar()

        println("Starting Deobfuscator.")
        for(transformer in transformers) {
            println("Running transformer: ${transformer::class.simpleName}.")
            transformer.run(group)
        }

        saveOutputJar()

        println("Deobfuscator completed successfully.")
    }

    private inline fun <reified T : Transformer> register() {
        transformers.add(T::class.createInstance())
    }
}