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

package org.spectralpowered.revtools.deobfuscator.ast

import org.spectralpowered.revtools.deobfuscator.Deobfuscator
import org.tinylog.kotlin.Logger
import java.io.File

class ASTDeobfuscator(
    private val sourcesDir: File
) : Deobfuscator {

    override fun run() {
        Logger.info("AST Deobfuscator not yet implemented. Skipping deobfuscator task.")
    }
}