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

import org.spectralpowered.revtools.ClassPool
import org.spectralpowered.revtools.deobfuscator.Deobfuscator
import org.spectralpowered.revtools.deobfuscator.bytecode.transformer.ControlFlowTransformer
import org.spectralpowered.revtools.deobfuscator.bytecode.transformer.DeadCodeTransformer
import org.spectralpowered.revtools.deobfuscator.bytecode.transformer.RenameTransformer
import org.spectralpowered.revtools.deobfuscator.bytecode.transformer.RuntimeExceptionTransformer
import org.tinylog.kotlin.Logger
import java.io.File
import kotlin.reflect.full.createInstance

class BytecodeDeobfuscator(
    private val inputJar: File,
    private val outputJar: File
) : Deobfuscator {

    val pool = ClassPool()
    private val transformers = mutableListOf<BytecodeTransformer>()

    private fun registerTransformers() {
        transformers.clear()

        // Register Bytecode Transformers
        // NOTE: Order of execution
        register<RuntimeExceptionTransformer>()
        register<DeadCodeTransformer>()
        register<ControlFlowTransformer>()
        register<RenameTransformer>()

        Logger.info("Registered ${transformers.size} bytecode transformers.")
    }

    override fun run() {
        // Setup / Prepare
        Logger.info("Preparing bytecode deobfuscator...")
        registerTransformers()
        loadClasses()

        // Run transformers
        Logger.info("Starting bytecode deobfuscator...")
        val start = System.currentTimeMillis()
        for(transformer in transformers) {
            Logger.info("Running transformer: ${transformer::class.simpleName}")
            transformer.run(pool)
            transformer.postRun()
        }
        val delta = System.currentTimeMillis() - start
        Logger.info("Finished bytecode deobfuscator in ${(delta / 1000L)}s.")

        // Save Classes
        saveClasses()

        // Complete
        Logger.info("Bytecode deobfuscator finished without errors.")
    }

    private fun loadClasses() {
        Logger.info("Loading classes from input jar: ${inputJar.name}.")
        pool.readJar(inputJar)
        for(cls in pool.classes) {
            if(cls.name.startsWith("org/")) {
                pool.ignoreClass(cls)
            }
        }
        pool.init()
        Logger.info("Loaded ${pool.classes.size} classes.")
    }

    private fun saveClasses() {
        Logger.info("Saving classes to output jar: ${outputJar.name}.")
        pool.writeJar(outputJar, includeResources = false, includeIgnored = false)
        Logger.info("Saved ${pool.classes.size} classes.")
    }

    @DslMarker
    private annotation class BytecodeTransformerDsl

    @BytecodeTransformerDsl
    private inline fun <reified T : BytecodeTransformer> register() {
        transformers.add(T::class.createInstance())
    }
}