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

import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler
import org.objectweb.asm.tree.ClassNode
import org.spectralpowered.revtools.toBytes
import org.tinylog.kotlin.Logger
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files

object FernflowerDecompiler : Decompiler {

    var args = arrayOf(
        "-dgs=1",
        "-asc=1",
    )

    override fun decompileJar(sourceJar: File, outputZip: File) {
        Logger.info("Decompiling jar file: ${sourceJar.name}...")

        val tempDir = Files.createTempDirectory("revtoolstmp").toFile()
        tempDir.deleteOnExit()
        tempDir.mkdirs()

        val decompDir = tempDir.resolve("decomp/")
        decompDir.mkdirs()

        try {
            ConsoleDecompiler.main(arrayOf(*args, sourceJar.absolutePath, decompDir.absolutePath))
        } catch (ignored: StackOverflowError) { }
        catch (ignored: Exception) { }

        if(outputZip.exists()) outputZip.deleteRecursively()
        val decompJar = decompDir.listFiles()!!.first()
        decompJar.renameTo(outputZip)

        Logger.info("Successfully decompiled jar classes to zip file: ${outputZip.name}.")
    }

    override fun decompileClassNode(cls: ClassNode): String {
        Logger.info("Decompiling class: ${cls.name}.")

        val tempDir = Files.createTempDirectory("revtoolstmp").toFile()
        tempDir.deleteOnExit()
        tempDir.mkdirs()

        val decompDir = tempDir.resolve("decomp/")
        decompDir.mkdirs()

        val tempClass = File.createTempFile(cls.name, ".class")
        tempClass.deleteOnExit()

        var ex = ""
        try {
            tempClass.outputStream().use { output ->
                output.write(cls.toBytes())
            }
        } catch (e: Exception) {
            val exWriter = StringWriter()
            e.printStackTrace(PrintWriter(exWriter))
            e.printStackTrace()
            ex = exWriter.toString()
        }

        try {
            ConsoleDecompiler.main(arrayOf(*args, tempClass.absolutePath, decompDir.absolutePath))
        } catch (e: Throwable) {
            val exwriter = StringWriter()
            e.printStackTrace(PrintWriter(exwriter))
            e.printStackTrace()
            ex = exwriter.toString()
        }

        val decompClass = decompDir.resolve("${cls.name}.java")
        if(!decompClass.exists()) {
            ex = "Failed to decompile class: ${cls.name}. ${decompClass.absolutePath} was not found."
        }

        val result = decompClass.bufferedReader().readText()
        tempClass.deleteRecursively()
        decompClass.deleteRecursively()

        if(ex.isNotBlank()) {
            error(ex)
        }

        return result
    }
}