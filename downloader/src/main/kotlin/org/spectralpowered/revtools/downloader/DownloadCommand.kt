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

package org.spectralpowered.revtools.downloader

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import org.tinylog.kotlin.Logger
import java.io.File

class DownloadCommand : CliktCommand(
    name = "download",
    help = "Downloads the Old School RuneScape gamepack jar",
    printHelpOnEmptyArgs = true
) {

    private val outputFile by option("--output", "-o", help = "Output file path")
        .file(canBeDir = false)
        .default(File("gamepack.jar"))

    private val url by option("--url", "-u", help = "Jagex base URL")
        .default("https://oldschool.runescape.com")

    override fun run() {
        Logger.info("Downloading latest Old School RuneScape gamepack jar...")

        val javConfig = JavConfig("$url/jav_config.ws")
        Logger.info("Current Revision: ${javConfig["param_25"]}")

        val gamepack = Gamepack(javConfig)
        gamepack.save(outputFile)
        Logger.info("Saved gamepack jar file: ${outputFile.path}.")
    }
}