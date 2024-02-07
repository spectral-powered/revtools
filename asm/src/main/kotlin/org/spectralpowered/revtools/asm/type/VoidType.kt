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

object VoidType : Type() {
    override val bitSize: Int
        get() = throw IllegalAccessError()

    override val name = "void"
    override val isPrimitive get() = false
    override val isVoid get() = true
    override val asmDesc get() = "V"

    override fun hashCode() = name.hashCode()
    override fun equals(other: Any?) = this === other
    override fun toString(): String = name

    override val isConcrete get() = true
    override fun isSubtypeOf(other: Type, outerClassBehavior: Boolean) = false
}
