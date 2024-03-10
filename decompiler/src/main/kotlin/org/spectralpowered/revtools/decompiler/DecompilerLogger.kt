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

package org.spectralpowered.revtools.decompiler

import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger
import org.tinylog.kotlin.Logger

object DecompilerLogger : IFernflowerLogger() {

    override fun startClass(className: String) {
        Logger.debug("Decompiling Class: $className")
    }

    override fun writeMessage(message: String, level: Severity) {
        when(level) {
            Severity.TRACE -> Logger.trace(message)
            Severity.INFO -> Logger.debug(message)
            Severity.WARN -> Logger.debug(message)
            Severity.ERROR -> Logger.error(message)
        }
    }

    override fun writeMessage(message: String, level: Severity, e: Throwable) {
        when(level) {
            Severity.TRACE -> Logger.trace(message, e)
            Severity.INFO -> Logger.debug(message, e)
            Severity.WARN -> Logger.debug(message, e)
            Severity.ERROR -> Logger.error(message, e)
        }
    }
}