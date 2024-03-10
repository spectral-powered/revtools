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

package org.spectralpowered.revtools.decompiler

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import org.jetbrains.java.decompiler.main.Fernflower
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences
import java.nio.file.Files
import java.util.jar.JarFile

class Decompiler : CliktCommand(
    name = "decompile",
    invokeWithoutSubcommand = true,
    printHelpOnEmptyArgs = true
) {

    private val options = mapOf(
        IFernflowerPreferences.INDENT_STRING to "\t",
        IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES to "1",
        IFernflowerPreferences.ASCII_STRING_CHARACTERS to "1"
    )

    private val inputJar by argument("input-jar", help = "Input jar path")
        .file(mustExist = true)

    private val outputDir by argument("output-dir", help = "Output directory path")
        .file(canBeDir = true)

    override fun run() {
        if(outputDir.exists()) outputDir.deleteRecursively()
        outputDir.mkdirs()

        DecompilerProcessor(outputDir.toPath()).use { processor ->
            val fernflower = Fernflower(processor, processor, options, DecompilerLogger)
            fernflower.addSource(inputJar)
            fernflower.decompileContext()
        }

        outputDir.walkTopDown().forEach { file ->
            if(file.name.endsWith(".class")) {
                file.delete()
            }
        }
    }
}

fun main(args: Array<String>) = Decompiler().main(args)