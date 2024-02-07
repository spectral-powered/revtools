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

package org.spectralpowered.revtools.asm.ir

import org.spectralpowered.revtools.asm.Package

@Suppress("MemberVisibilityCanBePrivate")
data class Location(val pkg: Package, val file: String, val line: Int) {
    companion object {
        val UNKNOWN_PACKAGE = Package.defaultPackage
        const val UNKNOWN_SOURCE = "unknown"
        const val UNKNOWN_LINE = -1
    }

    constructor() : this(UNKNOWN_PACKAGE, UNKNOWN_SOURCE, UNKNOWN_LINE)

    val isKnown
        get() = pkg != UNKNOWN_PACKAGE && file != UNKNOWN_SOURCE && line != UNKNOWN_LINE

    override fun toString() = when {
        isKnown -> "$pkg/$file:$line"
        else -> UNKNOWN_SOURCE
    }
}
