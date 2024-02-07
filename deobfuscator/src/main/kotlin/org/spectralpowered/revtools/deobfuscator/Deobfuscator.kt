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

package org.spectralpowered.revtools.deobfuscator

import org.spectralpowered.revtools.deobfuscator.asm.tree.ClassGroup
import org.spectralpowered.revtools.deobfuscator.transformer.*
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
        register<RedundantGotoRemover>()
        register<UnusedTryCatchRemover>()
        register<ControlFlowOptimizer>()
        register<UniqueRenamer>()
        register<UnusedMethodRemover>()
        register<UnusedArgRemover>()
        register<UnusedFieldRemover>()
        //register<StackFrameOptimizer>()
        //register<MultiplierRemover>()

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