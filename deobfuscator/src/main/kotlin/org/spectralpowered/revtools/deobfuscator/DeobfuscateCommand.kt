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

package org.spectralpowered.revtools.deobfuscator

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import org.jboss.windup.util.ZipUtil
import org.spectralpowered.revtools.decompiler.FernflowerDecompiler
import org.spectralpowered.revtools.deobfuscator.ast.ASTDeobfuscator
import org.spectralpowered.revtools.deobfuscator.bytecode.BytecodeDeobfuscator
import org.tinylog.kotlin.Logger
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile

class DeobfuscateCommand : CliktCommand(
    name = "deobfuscate",
    help = "Deobfuscates the bytecode and AST to cleanup the vanilla gamepack jar for decompilation",
    printHelpOnEmptyArgs = true
) {

    private val inputJar by argument("Input Jar", help = "Input (vanilla) gamepack jar path")
        .file(canBeDir = false, mustExist = true)
        .validate { it.name.endsWith(".jar") }

    private val outputJar by argument("Output Jar", help = "Output (deobfuscated) gamepack jar path")
        .file(canBeDir = false)
        .validate { it.name.endsWith(".jar") }

    private val noAstDeob by option("--no-ast", "-na", help = "Disables the AST deobfuscator.")
        .flag(default = false)

    private val noBytecodeDeob by option("--no-bytecode", "-nb", help = "Disables the bytecode deobfuscator.")
        .flag(default = false)

    private val runTestClient by option("--test-client", "-tc", help = "Runs a test client using the output deobfuscated jar.")
        .flag(default = false)

    override fun run() {
        Logger.info("RevTools Deobfuscator (Spectral-Powered)")

        if(!noBytecodeDeob) {
            // Run the bytecode deobfuscator.
            BytecodeDeobfuscator(inputJar, outputJar).run()
        } else {
            Files.deleteIfExists(outputJar.toPath())
            if(outputJar.parentFile?.exists() != true) {
                outputJar.mkdirs()
            }
            inputJar.copyTo(outputJar)
        }

        if(!noAstDeob) {
            // Decompile output jar
            val decompZip = outputJar.parentFile!!.resolve(outputJar.nameWithoutExtension+".decomp.zip")
            FernflowerDecompiler.decompileJar(outputJar, decompZip)

            // Extract decomp zip to dir.
            Logger.info("Extracting decompiled java files...")
            val decompDir = decompZip.parentFile!!.resolve("decomp/")
            decompDir.deleteRecursively()
            if(decompDir.parentFile?.exists() != true) {
                decompDir.mkdirs()
            }
            ZipFile(decompZip).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    zip.getInputStream(entry).use { input ->
                        if(entry.isDirectory) {
                            val entryDir = decompDir.resolve(entry.name)
                            if(!entryDir.exists()) entryDir.mkdirs()
                        } else {
                            val entryFile = decompDir.resolve(entry.name)
                            if(entryFile.parentFile?.exists() != true) entryFile.parentFile?.mkdirs()
                            entryFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
            decompZip.deleteRecursively()

            // Run the AST deobfuscator
            ASTDeobfuscator(decompDir).run()
        }

        // Run the test client if enabled
        if(runTestClient) {
            Logger.info("Starting test client using jar: ${outputJar.name}...")
            TestClient(outputJar).start()
            Logger.info("Test client has exited.")
        }

        Logger.info("RevTools deobfuscator has successfully completed. The results saved to: '${outputJar.path}'.")
    }
}