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

package org.spectralpowered.revtools.asm.helper.logging

import org.slf4j.Logger
import org.slf4j.Marker

@Suppress("unused")
class NullLogger : Logger {
    override fun getName(): String = "null"
    override fun isTraceEnabled(): Boolean = false
    override fun isTraceEnabled(p0: Marker?): Boolean = false
    override fun trace(p0: String?) {}
    override fun trace(p0: String?, p1: Any?) {}
    override fun trace(p0: String?, p1: Any?, p2: Any?) {}
    override fun trace(p0: String?, vararg p1: Any?) {}
    override fun trace(p0: String?, p1: Throwable?) {}
    override fun trace(p0: Marker?, p1: String?) {}
    override fun trace(p0: Marker?, p1: String?, p2: Any?) {}
    override fun trace(p0: Marker?, p1: String?, p2: Any?, p3: Any?) {}
    override fun trace(p0: Marker?, p1: String?, vararg p2: Any?) {}
    override fun trace(p0: Marker?, p1: String?, p2: Throwable?) {}
    override fun isDebugEnabled(): Boolean = false
    override fun isDebugEnabled(p0: Marker?): Boolean = false
    override fun debug(p0: String?) {}
    override fun debug(p0: String?, p1: Any?) {}
    override fun debug(p0: String?, p1: Any?, p2: Any?) {}
    override fun debug(p0: String?, vararg p1: Any?) {}
    override fun debug(p0: String?, p1: Throwable?) {}
    override fun debug(p0: Marker?, p1: String?) {}
    override fun debug(p0: Marker?, p1: String?, p2: Any?) {}
    override fun debug(p0: Marker?, p1: String?, p2: Any?, p3: Any?) {}
    override fun debug(p0: Marker?, p1: String?, vararg p2: Any?) {}
    override fun debug(p0: Marker?, p1: String?, p2: Throwable?) {}
    override fun isInfoEnabled(): Boolean  = false
    override fun isInfoEnabled(p0: Marker?): Boolean = false
    override fun info(p0: String?) {}
    override fun info(p0: String?, p1: Any?) {}
    override fun info(p0: String?, p1: Any?, p2: Any?) {}
    override fun info(p0: String?, vararg p1: Any?) {}
    override fun info(p0: String?, p1: Throwable?) {}
    override fun info(p0: Marker?, p1: String?) {}
    override fun info(p0: Marker?, p1: String?, p2: Any?) {}
    override fun info(p0: Marker?, p1: String?, p2: Any?, p3: Any?) {}
    override fun info(p0: Marker?, p1: String?, vararg p2: Any?) {}
    override fun info(p0: Marker?, p1: String?, p2: Throwable?) {}
    override fun isWarnEnabled(): Boolean = false
    override fun isWarnEnabled(p0: Marker?): Boolean = false
    override fun warn(p0: String?) {}
    override fun warn(p0: String?, p1: Any?) {}
    override fun warn(p0: String?, vararg p1: Any?) {}
    override fun warn(p0: String?, p1: Any?, p2: Any?) {}
    override fun warn(p0: String?, p1: Throwable?) {}
    override fun warn(p0: Marker?, p1: String?) {}
    override fun warn(p0: Marker?, p1: String?, p2: Any?) {}
    override fun warn(p0: Marker?, p1: String?, p2: Any?, p3: Any?) {}
    override fun warn(p0: Marker?, p1: String?, vararg p2: Any?) {}
    override fun warn(p0: Marker?, p1: String?, p2: Throwable?) {}
    override fun isErrorEnabled(): Boolean = false
    override fun isErrorEnabled(p0: Marker?): Boolean = false
    override fun error(p0: String?) {}
    override fun error(p0: String?, p1: Any?) {}
    override fun error(p0: String?, p1: Any?, p2: Any?) {}
    override fun error(p0: String?, vararg p1: Any?) {}
    override fun error(p0: String?, p1: Throwable?) {}
    override fun error(p0: Marker?, p1: String?) {}
    override fun error(p0: Marker?, p1: String?, p2: Any?) {}
    override fun error(p0: Marker?, p1: String?, p2: Any?, p3: Any?) {}
    override fun error(p0: Marker?, p1: String?, vararg p2: Any?) {}
    override fun error(p0: Marker?, p1: String?, p2: Throwable?) {}
}
