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

import org.jsoup.Jsoup
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributeView
import java.util.jar.JarFile

class Gamepack(private val javConfig: JavConfig) {

    fun save(file: File) {
        val bytes = Jsoup.connect(javConfig["codebase"] + javConfig["initial_jar"])
            .maxBodySize(0)
            .ignoreContentType(true)
            .execute()
            .bodyAsBytes()
        Files.deleteIfExists(file.toPath())
        if(file.parentFile?.exists() == false) {
            file.parentFile!!.mkdirs()
        }
        Files.write(file.toPath(), bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        val jar = JarFile(file)
        val time = jar.getJarEntry(javConfig["initial_class"]).lastModifiedTime
        val attr = Files.getFileAttributeView(file.toPath(), BasicFileAttributeView::class.java)
        attr.setTimes(time, time, time)
        jar.close()
    }

}