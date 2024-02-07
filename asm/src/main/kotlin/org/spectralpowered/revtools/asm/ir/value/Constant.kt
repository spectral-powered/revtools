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

sealed class Constant(name: String, type: Type) : Value(ConstantName(name), type)

class BoolConstant(val value: Boolean, type: Type) : Constant(value.toString(), type) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BoolConstant

        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()
}

class ByteConstant(val value: Byte, type: Type) : Constant(value.toString(), type) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ByteConstant

        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()
}

class ShortConstant(val value: Short, type: Type) : Constant(value.toString(), type) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShortConstant

        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()
}

class IntConstant(val value: Int, type: Type) : Constant(value.toString(), type) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IntConstant

        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()
}

class LongConstant(val value: Long, type: Type) : Constant(value.toString(), type) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LongConstant

        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()
}

class CharConstant(val value: Char, type: Type) : Constant(value.toString(), type) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CharConstant

        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()
}

class FloatConstant(val value: Float, type: Type) : Constant("${value}f", type) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FloatConstant

        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()
}

class DoubleConstant(val value: Double, type: Type) : Constant(value.toString(), type) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DoubleConstant

        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()
}

class StringConstant(val value: String, type: Type) : Constant("\"$value\"", type) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StringConstant

        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()
}

class MethodConstant(val method: Method, type: Type) : Constant(method.name, type) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MethodConstant

        return method == other.method
    }

    override fun hashCode(): Int = method.hashCode()
}

class ClassConstant(type: Type, val constantType: Type) : Constant("${constantType.name}.class", type)

class NullConstant(type: Type) : Constant("null", type)
