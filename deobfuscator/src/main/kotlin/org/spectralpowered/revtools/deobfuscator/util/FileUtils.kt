package org.spectralpowered.revtools.deobfuscator.util

import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.jar.JarFile

val JarFile.classLoader get() = File(this.name).classLoader
val File.classLoader get() = URLClassLoader(arrayOf(toURI().toURL()))
val Path.classLoader get() = this.toFile().classLoader