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

package org.spectralpowered.revtools.asm.type

import org.spectralpowered.revtools.asm.Package

abstract class Type {
    companion object {
        const val WORD = 32
        const val DWORD = 64
    }

    abstract val name: String

    abstract val asmDesc: String

    abstract val isPrimitive: Boolean

    open val isDWord
        get() = false

    open val isVoid
        get() = false

    open val isInteger
        get() = false

    open val isReal
        get() = false

    open val isReference
        get() = false

    val canonicalDesc
        get() = asmDesc.replace(Package.SEPARATOR, Package.CANONICAL_SEPARATOR)

    abstract val bitSize: Int

    abstract val isConcrete: Boolean
    abstract fun isSubtypeOf(other: Type, outerClassBehavior: Boolean = true): Boolean
    fun isSupertypeOf(other: Type, outerClassBehavior: Boolean = true): Boolean =
        other.isSubtypeOf(this, outerClassBehavior)

    val asArray: ArrayType by lazy {
        ArrayType(this)
    }
}
