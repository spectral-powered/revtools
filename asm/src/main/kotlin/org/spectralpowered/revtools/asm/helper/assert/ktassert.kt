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

@file:Suppress("NOTHING_TO_INLINE", "unused")

package org.spectralpowered.revtools.asm.helper.assert

import org.spectralpowered.revtools.asm.helper.KtException
import kotlin.system.exitProcess

class UnreachableException(message: String) : KtException(message)
class AssertionException(message: String) : KtException(message) {
    constructor() : this("")
}

inline fun <T> asserted(condition: Boolean, action: () -> T): T {
    ktassert(condition)
    return action()
}

inline fun <T> asserted(condition: Boolean, message: String, action: () -> T): T {
    ktassert(condition, message)
    return action()
}

inline fun ktassert(cond: Boolean) = if (!cond) throw AssertionException() else {
}

inline fun ktassert(cond: Boolean, message: String) = if (!cond) throw AssertionException(
    message
) else {
}

inline fun ktassert(cond: Boolean, action: () -> Unit) = if (!cond) {
    action()
    throw AssertionException()
} else {
}

inline fun <T> unreachable(message: String): T = fail(message)
inline fun <T> unreachable(noinline lazyMessage: () -> Any) =
    fail<T>(lazyMessage)

inline fun exit(message: String) = exit<Unit>(message)
inline fun exit(lazyMessage: () -> Any) = exit<Unit>(lazyMessage)

inline fun <T> exit(message: String): T = exit<T> { println(message) }
inline fun <T> exit(lazyMessage: () -> Any): T {
    lazyMessage()
    exitProcess(0)
}

fun <T> fail(message: String): T = error(message)
fun <T> fail(lazyMessage: () -> Any): T {
    lazyMessage()
    error("Failure")
}
