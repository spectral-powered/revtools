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
import org.spectralpowered.revtools.asm.ir.Class
import org.spectralpowered.revtools.asm.ir.ConcreteClass
import org.spectralpowered.revtools.asm.util.Flags
import org.spectralpowered.revtools.asm.util.JarBuilder
import org.spectralpowered.revtools.asm.util.classLoader
import org.spectralpowered.revtools.asm.util.hasFrameInfo
import org.spectralpowered.revtools.asm.util.isClass
import org.spectralpowered.revtools.asm.util.isManifest
import org.spectralpowered.revtools.asm.util.longestCommonPrefix
import org.spectralpowered.revtools.asm.util.pkg
import org.spectralpowered.revtools.asm.util.readClassNode
import org.spectralpowered.revtools.asm.util.recomputeFrames
import org.spectralpowered.revtools.asm.util.write
import org.spectralpowered.revtools.asm.helper.`try`
import java.io.File
import java.io.FileInputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.Manifest
import kotlin.io.path.writeBytes

@Suppress("unused")
class JarContainer(override val path: Path, pkg: Package? = null) : Container {
    private val file = JarFile(path.toFile())
    private val manifest = Manifest()

    constructor(path: String, `package`: Package?) : this(Paths.get(path), `package`)
    constructor(path: String, `package`: String) : this(Paths.get(path), Package.parse(`package`))

    override fun toString(): String = path.toString()

    override val pkg: Package = pkg ?: commonPackage
    override val name: String get() = file.name
    override val classLoader get() = file.classLoader

    override val commonPackage: Package
        get() {
            val klasses = mutableListOf<String>()
            val enumeration = file.entries()
            while (enumeration.hasMoreElements()) {
                val entry = enumeration.nextElement() as JarEntry

                if (entry.isClass) {
                    klasses += entry.name
                }

            }
            val commonSubstring = longestCommonPrefix(klasses).dropLastWhile { it != Package.SEPARATOR }
            return Package.parse("$commonSubstring*")
        }

    init {
        if (file.manifest != null) {
            for ((key, value) in file.manifest.mainAttributes) {
                manifest.mainAttributes[key] = value
            }
            for ((key, value) in file.manifest.entries) {
                manifest.entries[key] = value
            }
        }
    }

    private inline fun <T> failSafeAction(failOnError: Boolean, action: () -> T): T? = `try`<T?> {
        action()
    }.getOrElse {
        if (failOnError) throw UnsupportedCfgException()
        else null
    }

    override fun parse(flags: Flags, failOnError: Boolean, loader: ClassLoader): Map<String, ClassNode> {
        val classes = mutableMapOf<String, ClassNode>()
        val enumeration = file.entries()
        while (enumeration.hasMoreElements()) {
            val entry = enumeration.nextElement() as JarEntry

            if (entry.isClass && pkg.isParent(entry.pkg)) {
                val classNode = readClassNode(file.getInputStream(entry), flags)

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
        val enumeration = file.entries()
        val allClasses = group.getContainerClasses(this)
        val visitedClasses = mutableSetOf<Class>()

        while (enumeration.hasMoreElements()) {
            val entry = enumeration.nextElement() as JarEntry
            if (entry.isManifest) continue

            if (entry.isClass) {
                failSafeAction(failOnError) {
                    val `class` = group[entry.name.removeSuffix(".class")]
                    visitedClasses += `class`
                    when {
                        pkg.isParent(entry.pkg) && `class` is ConcreteClass -> {
                            val path = absolutePath.resolve(Paths.get(`class`.pkg.fileSystemPath, "${`class`.name}.class"))
                            `class`.write(group, loader, path, Flags.writeComputeFrames, checkClass)
                        }
                        unpackAllClasses -> {
                            val path = absolutePath.resolve(entry.name)
                            val classNode = readClassNode(file.getInputStream(entry))
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

    override fun update(group: ClassGroup, target: Path, loader: ClassLoader): JarContainer {
        @Suppress("BooleanLiteralArgument")
        unpack(group, target, false, false, false, loader)

        val absolutePath = target.toAbsolutePath()
        val jarName = file.name.substringAfterLast(File.separator).removeSuffix(".jar")
        val jarPath = absolutePath.resolve("$jarName.jar")
        val builder = JarBuilder("$jarPath", manifest)
        val enumeration = file.entries()

        while (enumeration.hasMoreElements()) {
            val entry = enumeration.nextElement() as JarEntry
            if (entry.isManifest) continue

            if (entry.isClass && pkg.isParent(entry.pkg)) {
                val `class` = group[entry.name.removeSuffix(".class")]

                if (`class` is ConcreteClass) {
                    val localPath = "${`class`.fullName}.class"
                    val path = "${absolutePath.resolve(localPath)}"

                    val newEntry = JarEntry(localPath.replace("\\", "/"))
                    builder.add(newEntry, FileInputStream(path))
                } else {
                    builder.add(entry, file.getInputStream(entry))
                }
            } else {
                builder.add(entry, file.getInputStream(entry))
            }
        }
        builder.close()
        return JarContainer(builder.name, pkg)
    }

    override fun extract(target: Path) {
        val enumeration = file.entries()

        while (enumeration.hasMoreElements()) {
            val entry = enumeration.nextElement() as JarEntry

            if (entry.isClass) {
                val bytes = file.getInputStream(entry).readBytes()
                val copyFile = target.resolve(entry.name).also {
                    it.toFile().parentFile?.mkdirs()
                }
                copyFile.writeBytes(bytes)
            }
        }
    }
}
