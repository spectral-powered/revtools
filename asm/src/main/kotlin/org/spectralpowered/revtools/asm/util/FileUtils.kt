package org.spectralpowered.revtools.asm.util

import java.io.File
import java.net.URLClassLoader
import java.util.ArrayDeque

val File.isJar get() = this.name.endsWith(".jar")
val File.isClass get() = this.name.endsWith(".class")
val File.className get() = this.name.removeSuffix(".class")

val File.classLoader get() = URLClassLoader(arrayOf(toURI().toURL()))

val File.allEntries: List<File> get() {
    val ret = mutableListOf<File>()
    val queue = ArrayDeque<File>().also { it.add(this) }
    while(queue.isNotEmpty()) {
        val cur = queue.poll()
        if(cur.isFile) {
            ret.add(cur)
        } else if(cur.isDirectory) {
            queue.addAll(cur.listFiles() ?: emptyArray())
        }
    }
    return ret
}