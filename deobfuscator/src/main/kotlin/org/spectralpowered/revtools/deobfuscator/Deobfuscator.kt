package org.spectralpowered.revtools.deobfuscator

import org.spectralpowered.revtools.deobfuscator.asm.tree.ClassGroup
import org.spectralpowered.revtools.deobfuscator.transformer.AddDeobClasses
import org.spectralpowered.revtools.deobfuscator.transformer.IllegalStateExceptionRemover
import org.spectralpowered.revtools.deobfuscator.transformer.RuntimeExceptionRemover
import org.spectralpowered.revtools.deobfuscator.transformer.UniqueRenamer
import java.io.File
import kotlin.reflect.full.createInstance

class Deobfuscator(
    private val inputJar: File,
    private val outputJar: File
) {

    private val group = ClassGroup()
    private val transformers = mutableListOf<Transformer>()

    private fun init() {
        group.clear()
        transformers.clear()
    }

    private fun registerTransformers() {
        /**
         * === TRANSFORMERS ===
         * NOTE: The register order is IMPORTANT and will be the run order of the transformers.
         */

        register<AddDeobClasses>()
        register<RuntimeExceptionRemover>()
        register<IllegalStateExceptionRemover>()
        register<UniqueRenamer>()

        /**
         * === END TRANSFORMERS ===
         */
        Logger.info("Registered ${transformers.size} transformers.")
    }

    fun run() {
        Logger.info("Preparing Deobfuscator.")

        init()
        registerTransformers()
        loadInputJar()

        Logger.info("Starting Deobfuscator.")
        val start = System.currentTimeMillis()
        transformers.forEach { transformer ->
            Logger.info("Running transformer: '${transformer::class.simpleName}'.")
            transformer.run(group)
        }
        val end = System.currentTimeMillis()
        Logger.info("Deobfuscator finished in ${(end - start)/1000L}s.")

        writeOutputJar()

        Logger.info("Deobfuscator completed successfully.")
    }

    private fun loadInputJar() {
        Logger.info("Loading classes from input jar: '${inputJar.name}'.")
        group.readJar(inputJar)
        group.ignoreClassIf { it.name.startsWith("org/") }
        group.build()
        Logger.info("Successfully loaded ${group.classes.size} classes.")
    }

    private fun writeOutputJar() {
        Logger.info("Writing classes to output jar: '${outputJar.name}'.")
        group.writeJar(outputJar, includeIgnored = true, includeResources = false)
        Logger.info("Successfully wrote ${(group.classes.size + group.ignoredClasses.size)} classes.")
    }

    private inline fun <reified T : Transformer> register() {
        transformers.add(T::class.createInstance())
    }
}