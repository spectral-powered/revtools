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

package org.spectralpowered.revtools.asm.util

import kotlin.math.min

private infix fun String.lcp(other: String): String {
    var indx = 0
    val max = min(this.length, other.length)
    while (indx < max) {
        if (this[indx] != other[indx]) break
        ++indx
    }
    return this.substring(0, indx)
}

// very bad implementation
internal fun longestCommonPrefix(strings: List<String>): String {
    if (strings.isEmpty()) return ""
    if (strings.size == 1) return strings.first()
    var prefix = strings[0] lcp strings[1]
    for (i in 2 until strings.size) {
        prefix = prefix lcp strings[i]
    }
    return prefix
}