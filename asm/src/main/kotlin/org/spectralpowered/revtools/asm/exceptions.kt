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

package org.spectralpowered.revtools.asm

abstract class AsmException : Exception {
    constructor() : super()
    constructor(msg: String) : super(msg)

    @Suppress("unused")
    constructor(msg: String, reason: Throwable) : super(msg, reason)
    constructor(reason: Throwable) : super(reason)

    override fun toString(): String = "${this.javaClass.kotlin} : $message"
}

@Deprecated("not used")
class InvalidTypeException(msg: String) : AsmException(msg)

class InvalidOpcodeException(msg: String) : AsmException(msg)

class InvalidOperandException(msg: String) : AsmException(msg)

class InvalidStateException(msg: String) : AsmException(msg)

class UnknownInstanceException(msg: String) : AsmException(msg)

class UnsupportedOperationException(msg: String) : AsmException(msg)

class UnsupportedCfgException(msg: String) : AsmException(msg) {
    constructor() : this("")
}
