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
import org.tinylog.kotlin.Logger
import java.io.File
import java.nio.file.Files

object FernflowerDecompiler : Decompiler {

    var args = arrayOf(
        "-dgs=1",
        "-asc=1",
        "-hdc=0"
    )

    override fun decompileJar(sourceJar: File, outputZip: File) {
        Logger.info("Decompiling jar file: ${sourceJar.name}...")

        val tempDir = Files.createTempDirectory("revtoolstmp").toFile()
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
}