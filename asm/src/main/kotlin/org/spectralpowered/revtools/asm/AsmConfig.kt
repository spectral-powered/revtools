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

import org.spectralpowered.revtools.asm.util.Flags
import org.spectralpowered.revtools.asm.helper.KtException
import org.spectralpowered.revtools.asm.helper.assert.ktassert

class InvalidAsmConfigException(msg: String) : KtException(msg)

data class AsmConfig(
        val flags: Flags = Flags.readAll,
        val useCachingLoopManager: Boolean = false,
        val failOnError: Boolean = true,
        val verifyIR: Boolean = false,
        val checkClasses: Boolean = false
) {

    init {
        ktassert(flags < Flags.readSkipFrames, "Can't create config with 'skipFrames' option")
    }
}

@Suppress("unused")
class AsmConfigBuilder private constructor(private val current: AsmConfig) {
    constructor() : this(AsmConfig())

    fun flags(flags: Flags) = AsmConfigBuilder(current.copy(flags = flags))
    fun failOnError(value: Boolean) = AsmConfigBuilder(current.copy(failOnError = value))
    fun verifyIR(value: Boolean) = AsmConfigBuilder(current.copy(verifyIR = value))
    fun checkClasses(value: Boolean) = AsmConfigBuilder(current.copy(checkClasses = value))
    fun useCachingLoopManager(value: Boolean) = AsmConfigBuilder(current.copy(useCachingLoopManager = value))

    fun build() = current
}
