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
import org.spectralpowered.revtools.asm.UnsupportedCfgException
import org.spectralpowered.revtools.asm.helper.`try`
import org.spectralpowered.revtools.asm.helper.write
import org.spectralpowered.revtools.asm.ir.Class
import org.spectralpowered.revtools.asm.ir.ConcreteClass
import org.spectralpowered.revtools.asm.util.*
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.writeBytes

@Suppress("unused")
class DirectoryContainer(private val file: File, pkg: Package? = null) : Container {
    override val pkg: Package = pkg ?: commonPackage
    override val path: Path = file.toPath()

    override val name: String
        get() = file.absolutePath

    override val classLoader: ClassLoader
        get() = file.classLoader

    override val commonPackage: Package
        get() {
            val klasses = file.allEntries.filter { it.isClass }.map { it.fullClassName }
            val commonSubstring = longestCommonPrefix(klasses).dropLastWhile { it != Package.SEPARATOR }
            return Package.parse("$commonSubstring*")
        }

    constructor(path: Path, pkg: Package?) : this(path.toFile(), pkg)
    constructor(path: String, pkg: Package?) : this(Paths.get(path), pkg)
    constructor(path: String, pkg: String) : this(Paths.get(path), Package.parse(pkg))

    override fun toString(): String = path.toString()

    private val File.fullClassName: String get() = this.relativeTo(file).path.removeSuffix(".class")
    private val File.pkg: Package get() = Package(fullClassName.dropLastWhile { it != Package.SEPARATOR })

    private inline fun <T> failSafeAction(failOnError: Boolean, action: () -> T): T? = `try`<T?> {
        action()
    }.getOrElse {
        if (failOnError) throw UnsupportedCfgException()
        else null
    }

    override fun parse(flags: Flags, failOnError: Boolean, loader: ClassLoader): Map<String, ClassNode> {
        val classes = mutableMapOf<String, ClassNode>()
        for (entry in file.allEntries) {
            if (entry.isClass && pkg.isParent(entry.pkg)) {
                val classNode = readClassNode(entry.inputStream(), flags)

                // need to recompute frames because sometimes original Jar classes don't contain frame info
                val newClassNode = when {
                    classNode.hasFrameInfo -> classNode
                    else -> failSafeAction(failOnError) { classNode.recomputeFrames(loader) }
                } ?: continue
                classes[classNode.name] = newClassNode
            }

        }
        return classes
    }

    override fun unpack(
        group: ClassGroup,
        target: Path,
        unpackAllClasses: Boolean,
        failOnError: Boolean,
        checkClass: Boolean,
        loader: ClassLoader
    ) {
        val absolutePath = target.toAbsolutePath()
        val allClasses = group.getContainerClasses(this)
        val visitedClasses = mutableSetOf<Class>()

        for (entry in file.allEntries) {
            if (entry.isClass) {
                failSafeAction(failOnError) {
                    val `class` = group[entry.fullClassName]
                    visitedClasses += `class`
                    when {
                        pkg.isParent(entry.pkg) && `class` is ConcreteClass -> {
                            val path =
                                absolutePath.resolve(Paths.get(`class`.pkg.fileSystemPath, "${`class`.name}.class"))
                            `class`.write(group, loader, path, Flags.writeComputeFrames, checkClass)
                        }

                        unpackAllClasses -> {
                            val path = absolutePath.resolve(entry.fullClassName)
                            val classNode = readClassNode(entry.inputStream())
                            classNode.write(loader, path, Flags.writeComputeNone, checkClass)
                        }

                        else -> Unit
                    }
                }
            }
        }

        for (newKlass in allClasses.filter { it !in visitedClasses }) {
            when {
                pkg.isParent(newKlass.pkg) || unpackAllClasses -> {
                    val path = absolutePath.resolve(Paths.get(newKlass.pkg.fileSystemPath, "${newKlass.name}.class"))
                    failSafeAction(failOnError) { newKlass.write(group, loader, path, Flags.writeComputeFrames) }
                }
            }
        }
    }

    override fun update(group: ClassGroup, target: Path, loader: ClassLoader): Container {
        val absolutePath = target.toAbsolutePath()
        unpack(group, target)

        for (entry in file.allEntries) {
            if (entry.isClass && pkg.isParent(entry.pkg)) {
                val `class` = group[entry.fullClassName]

                if (`class` is ConcreteClass) {
                    val localName = "${`class`.pkg.fileSystemPath}${File.separator}${`class`.name}.class"

                    File(absolutePath.toString(), localName).write(entry.inputStream())
                }
            }
        }
        return DirectoryContainer(target.toFile(), pkg)
    }

    override fun extract(target: Path) {
        for (entry in file.allEntries) {

            if (entry.isClass) {
                val bytes = entry.readBytes()
                val copyFile = target.resolve(entry.relativeTo(this.file).toString())
                copyFile.parent.toFile().mkdirs()
                copyFile.writeBytes(bytes)
            }
        }
    }
}
