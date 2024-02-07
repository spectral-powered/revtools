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

package org.spectralpowered.revtools.asm.ir.value

import org.spectralpowered.revtools.asm.ir.Method
import org.spectralpowered.revtools.asm.type.Type

abstract class Value(val name: Name, val type: Type) : UsableValue() {
    val isNameDefined: Boolean
        get() = name !is UndefinedName

    val hasRealName get() = name is StringName
    override fun toString() = name.toString()

    override fun get() = this
}

class Argument(val index: Int, val method: Method, type: Type) : Value(ConstantName("$argPrefix$index"), type) {
    companion object {
        const val argPrefix = "arg\$"
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + method.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as Argument
        return this.index == other.index && this.type == other.type && this.method == other.method
    }
}

class ThisRef(type: Type) : Value(ConstantName("this"), type) {
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as ThisRef
        return this.type == other.type
    }
}
