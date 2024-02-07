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

package org.spectralpowered.revtools.asm.helper.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

val log: Logger
    inline get() = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

fun Logger.trace() = this.trace("")
fun <T> Logger.trace(t: T) = this.trace(t.toString())

fun Logger.info() = this.info("")
fun <T> Logger.info(t: T) = this.info(t.toString())

fun Logger.debug() = this.debug("")
fun <T> Logger.debug(t: T) = this.debug(t.toString())

fun Logger.warn() = this.warn("")
fun <T> Logger.warn(t: T) = this.warn(t.toString())

fun Logger.error() = this.error("")
fun <T> Logger.error(t: T) = this.error(t.toString())

inline fun Logger.debug(message: () -> String) =
    if (isDebugEnabled) debug(message()) else {
    }

inline fun Logger.trace(message: () -> String) =
    if (isTraceEnabled) trace(message()) else {
    }

inline fun Logger.info(message: () -> String) =
    if (isInfoEnabled) info(message()) else {
    }

inline fun Logger.warn(message: () -> String) =
    if (isWarnEnabled) warn(message()) else {
    }

inline fun Logger.error(message: () -> String) =
    if (isErrorEnabled) error(message()) else {
    }
