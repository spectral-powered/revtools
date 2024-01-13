package org.spectralpowered.revtools.deobfuscator.util

fun String.isDeobfuscatedName(): Boolean {
    return listOf("class", "method", "field").any { this.startsWith(it) }
}