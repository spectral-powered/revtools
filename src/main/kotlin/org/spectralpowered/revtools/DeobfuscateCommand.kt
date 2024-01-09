package org.spectralpowered.revtools

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import org.spectralpowered.revtools.deobfuscator.Deobfuscator

class DeobfuscateCommand : CliktCommand(
    name = "deobfuscate",
    help = "Deobfuscates the vanilla gamepack jar to a cleaner and decompiler friendly jar",
    printHelpOnEmptyArgs = true
) {

    private val inputJar by argument(
        name = "input-jar",
        help = "Path of obfuscated gamepack jar to deobfuscate"
    ).file(mustExist = true, canBeDir = false)

    private val outputJar by argument(
        name = "output-jar",
        help = "Path to save deobfuscated jar to"
    ).file(canBeDir = false)

    override fun run() {
        Deobfuscator(inputJar, outputJar).run()
    }
}