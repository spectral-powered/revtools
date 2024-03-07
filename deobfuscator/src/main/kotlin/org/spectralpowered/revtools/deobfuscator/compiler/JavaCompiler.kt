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

package org.spectralpowered.revtools.deobfuscator.compiler

import org.mdkt.compiler.InMemoryJavaCompiler
import java.io.File
import java.nio.file.Files

class JavaCompiler {

    private val classSources = hashMapOf<String, String>()
    private val compiler = InMemoryJavaCompiler.newInstance()

    fun addSourceDirectory(dir: File) {
        if(!dir.isDirectory) error("file arg is not a directory.")
        val javaFiles = dir.listFiles()!!.filter { it.name.endsWith(".java") }
        javaFiles.forEach { javaFile ->
            val src = javaFile.bufferedReader().readText()
            classSources[javaFile.toRelativeString(dir).replace(".java", "")] = src
        }
    }

    fun compile(dir: File) {
        dir.walkTopDown().forEach { f ->
            if(f.name.endsWith(".java")) {
                compiler.addSource(f.name.replace(".java", "").replace("/", "."), f.bufferedReader().readText())
            }
        }
        compiler.useParentClassLoader(JavaCompiler::class.java.classLoader)
        val res = compiler.compileAll()
        res.forEach { k, v -> println("Class: $k") }
    }
}