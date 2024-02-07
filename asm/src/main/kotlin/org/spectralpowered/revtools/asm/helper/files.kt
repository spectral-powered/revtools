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

@file:Suppress("unused")

package org.spectralpowered.revtools.asm.helper

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

private const val MAX_BYTE_ARRAY_SIZE = 16384

val InputStream.asByteArray: ByteArray
    get() {
        val buffer = ByteArrayOutputStream()

        var nRead: Int
        val data = ByteArray(MAX_BYTE_ARRAY_SIZE)

        while (this.read(data, 0, data.size).also { nRead = it } != -1) {
            buffer.write(data, 0, nRead)
        }

        return buffer.toByteArray()
    }

fun File.write(input: InputStream) = this.writeBytes(input.asByteArray)
