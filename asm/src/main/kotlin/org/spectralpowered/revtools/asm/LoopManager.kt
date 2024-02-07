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

import org.spectralpowered.revtools.asm.ir.Method
import org.spectralpowered.revtools.asm.visitor.Loop
import org.spectralpowered.revtools.asm.visitor.performLoopAnalysis

private class LoopInfo(val loops: List<Loop>, var valid: Boolean) {
    constructor() : this(listOf(), false)
    constructor(loops: List<Loop>) : this(loops, true)
}

internal interface LoopManager {
    fun invalidate()
    fun setInvalid(method: Method)
    fun getMethodLoopInfo(method: Method): List<Loop>
}

internal class DefaultLoopManager : LoopManager {
    override fun invalidate() {}
    override fun setInvalid(method: Method) {}
    override fun getMethodLoopInfo(method: Method) = performLoopAnalysis(method)
}

internal class CachingLoopManager(val group: ClassGroup) : LoopManager {
    private val loopInfo = mutableMapOf<Method, LoopInfo>()

    override fun invalidate() {
        for ((_, info) in loopInfo) {
            info.valid = false
        }
    }

    override fun setInvalid(method: Method) {
        loopInfo.getOrPut(method) { LoopInfo() }.valid = false
    }

    override fun getMethodLoopInfo(method: Method): List<Loop> {
        val info = loopInfo.getOrPut(method) { LoopInfo() }
        return when {
            info.valid -> info.loops
            else -> {
                val loops = performLoopAnalysis(method)
                loopInfo[method] = LoopInfo(loops)
                loops
            }
        }
    }
}