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

package org.spectralpowered.revtools.deobfuscator.bytecode

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import org.spectralpowered.revtools.asm.ClassPool
import org.spectralpowered.revtools.deobfuscator.bytecode.transformer.*
import org.tinylog.kotlin.Logger
import java.io.File

class Deobfuscator(
    private val inputJar: File,
    private val outputJar: File
) {

    private val pool = ClassPool()
    private val transformers = mutableListOf<Transformer>()

    private fun registerTransformers() {
        /*
         * Register bytecode transformers.
         * NOTE! The order here matters
         */
        register<RuntimeExceptionTransformer>()
        register<DeadCodeTransformer>()
        register<OpaquePredicateTransformer>()
        register<DeadCodeTransformer>()
        register<ControlFlowTransformer>()
        register<RenameTransformer>()
        register<FinalClassTransformer>()
        register<InvokeSpecialTransformer>()

        Logger.info("Registered ${transformers.size} bytecode transformers.")
    }

    fun run() {
        Logger.info("Preparing bytecode deobfuscator...")

        registerTransformers()
        loadClasses()

        Logger.info("Starting bytecode deobfuscator...")

        val start = System.currentTimeMillis()
        for(transformer in transformers) {
            Logger.info("Running transformer: ${transformer.name}.")
            transformer.transform(pool)
        }
        val delta = System.currentTimeMillis() - start
        Logger.info("Finished bytecode deobfuscator in ${delta/1000L} seconds.")

        writeClasses()

        Logger.info("Successfully completed bytecode deobfuscator.")
    }

    private fun loadClasses() {
        Logger.info("Loading classes from input jar: ${inputJar.name}.")

        pool.loadJar(inputJar)
        pool.build()
        pool.ignoreClasses { it.name.startsWith("org/") }

        Logger.info("Loaded ${pool.classes.size} classes. (Ignored ${pool.ignoredClasses.size})")
    }

    private fun writeClasses() {
        Logger.info("Writing deobfuscated classes to output jar: ${outputJar.name}.")
        pool.writeJar(outputJar, includeIgnored = false)
        Logger.info("Saved ${pool.classes.size} classes to jar.")
    }

    @DslMarker
    private annotation class TransformerDsl

    @TransformerDsl
    private inline fun <reified T : Transformer> register() {
        transformers.add(T::class.java.getDeclaredConstructor().newInstance())
    }

    private class Command : CliktCommand(
        name = "deobfuscate-bytecode",
        printHelpOnEmptyArgs = true,
        invokeWithoutSubcommand = true
    ) {

        private val inputJar by argument("input-jar", help = "Input Jar file path")
            .file(mustExist = true, canBeDir = false)

        private val outputJar by argument("output-jar", help = "Output jar file path")
            .file(canBeDir = false)

        private val runTestClient by option("--test-client", "-tc", help = "Runs a test client using output jar.")
            .flag(default = false)

        override fun run() {
            Deobfuscator(
                inputJar,
                outputJar
            ).run()

            if(runTestClient) {
                Logger.info("Starting test client using jar: ${outputJar.path}.")
                TestClient(outputJar).start()
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) = Command().main(args)
    }
}