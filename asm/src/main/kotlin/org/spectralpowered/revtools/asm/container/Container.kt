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

package org.spectralpowered.revtools.asm.container

import org.objectweb.asm.tree.ClassNode
import org.spectralpowered.revtools.asm.ClassGroup
import org.spectralpowered.revtools.asm.Package
import org.spectralpowered.revtools.asm.util.Flags
import org.spectralpowered.revtools.asm.util.isJar
import java.io.File
import java.nio.file.Path

interface Container {
    val name: String
    val pkg: Package
    val classLoader: ClassLoader
    val path: Path

    val commonPackage: Package

    fun parse(flags: Flags, failOnError: Boolean = false, loader: ClassLoader = classLoader): Map<String, ClassNode>
    fun unpack(
        group: ClassGroup,
        target: Path,
        unpackAllClasses: Boolean = false,
        failOnError: Boolean = false,
        checkClass: Boolean = false,
        loader: ClassLoader = classLoader
    )

    fun update(group: ClassGroup, target: Path, loader: ClassLoader = classLoader): Container

    fun extract(target: Path)
}

fun File.asContainer(pkg: Package? = null): Container? = when {
    this.isJar -> JarContainer(this.toPath(), pkg)
    this.isDirectory -> DirectoryContainer(this, pkg)
    else -> null
}

fun Path.asContainer(pkg: Package? = null) = this.toFile().asContainer(pkg)