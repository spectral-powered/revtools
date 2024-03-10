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

import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider
import org.jetbrains.java.decompiler.main.extern.IResultSaver
import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import java.util.jar.Manifest
import kotlin.io.path.bufferedWriter

class DecompilerProcessor(
    private val outputPath: Path
) : IBytecodeProvider, IResultSaver, Closeable {

    private val inputJars = mutableMapOf<String, JarFile>()

    override fun getBytecode(externalPath: String, internalPath: String?): ByteArray {
        if(internalPath == null) {
            throw UnsupportedOperationException()
        }

        val jar = inputJars.computeIfAbsent(externalPath) { JarFile(it) }

        jar.getInputStream(jar.getJarEntry(internalPath)).use {
            return it.readBytes()
        }
    }

    override fun saveFolder(p0: String) {}

    override fun copyFile(p0: String, p1: String, p2: String) {
        throw UnsupportedOperationException()
    }

    override fun saveClassFile(path: String, qualifiedName: String, entryName: String, content: String, mapping: IntArray) {
        throw UnsupportedOperationException()
    }

    override fun createArchive(p0: String, p1: String, p2: Manifest?) {}
    override fun saveDirEntry(p0: String, p1: String, p2: String) {}
    override fun copyEntry(p0: String, p1: String, p2: String, p3: String) {}
    override fun closeArchive(p0: String, p1: String) {}

    override fun saveClassEntry(path: String, archiveName: String, qualifiedName: String, entryName: String, content: String) {
        val filePath = outputPath.resolve(entryName)
        Files.createDirectories(filePath.parent)
        filePath.bufferedWriter().use { writer ->
            writer.write(content)
        }
    }

    override fun close() {
        for(jar in inputJars.values) jar.close()
    }
}