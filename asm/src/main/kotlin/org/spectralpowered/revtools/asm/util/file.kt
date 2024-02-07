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

package org.spectralpowered.revtools.asm.util

import org.spectralpowered.revtools.asm.helper.collection.queueOf
import java.io.File
import java.net.URLClassLoader


val File.isJar get() = this.name.endsWith(".jar")
val File.isClass get() = this.name.endsWith(".class")
val File.className get() = this.name.removeSuffix(".class")

val File.classLoader get() = URLClassLoader(arrayOf(toURI().toURL()))

val File.allEntries: List<File>
    get() {
        val result = mutableListOf<File>()
        val queue = queueOf(this)
        while (queue.isNotEmpty()) {
            val current = queue.poll()
            if (current.isFile) {
                result += current
            } else if (current.isDirectory) {
                queue.addAll(current.listFiles() ?: arrayOf())
            }
        }
        return result
    }
