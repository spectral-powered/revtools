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

import org.spectralpowered.revtools.asm.ClassGroup
import org.spectralpowered.revtools.asm.helper.assert.unreachable
import org.spectralpowered.revtools.asm.ir.Class
import org.spectralpowered.revtools.asm.ir.Method
import org.spectralpowered.revtools.asm.type.*

@Suppress("unused", "MemberVisibilityCanBePrivate")
class ValueFactory internal constructor(val group: ClassGroup) {
    val types get() = group.type

    fun getThis(type: Type): Value = ThisRef(type)
    fun getThis(klass: Class): Value = getThis(types.getRefType(klass))
    fun getArgument(index: Int, method: Method, type: Type): Value = Argument(index, method, type)

    // constants
    val nullConstant: Value
        get() = NullConstant(types.nullType)
    val trueConstant: Value
        get() = getBool(true)
    val falseConstant: Value
        get() = getBool(false)

    fun getBool(value: Boolean): Value = BoolConstant(value, types.boolType)
    fun getByte(value: Byte): Value = ByteConstant(value, types.byteType)
    fun getChar(value: Char): Value = CharConstant(value, types.charType)
    fun getShort(value: Short): Value = ShortConstant(value, types.shortType)
    fun getInt(value: Int): Value = IntConstant(value, types.intType)
    fun getLong(value: Long): Value = LongConstant(value, types.longType)
    fun getFloat(value: Float): Value = FloatConstant(value, types.floatType)
    fun getDouble(value: Double): Value = DoubleConstant(value, types.doubleType)
    fun getString(value: String): Value = StringConstant(value, types.stringType)
    fun getClass(desc: String): Value = ClassConstant(types.classType, parseDescOrNull(types, desc)!!)
    fun getClass(klass: Class): Value = ClassConstant(types.classType, types.getRefType(klass))
    fun getClass(type: Type): Value = ClassConstant(types.classType, type)
    fun getMethod(method: Method): Value = MethodConstant(method, types.getRefType("java/lang/invoke/MethodHandle"))

    fun getZero(type: Type): Value = when (type) {
        is BoolType -> getBool(false)
        is ByteType -> getByte(0.toByte())
        is CharType -> getChar(0.toChar())
        is ShortType -> getShort(0)
        is IntType -> getInt(0)
        is LongType -> getLong(0)
        is FloatType -> getFloat(0.0f)
        is DoubleType -> getDouble(0.0)
        is Reference -> nullConstant
        else -> unreachable("Unknown type: ${type.name}")
    }

    fun getNumber(value: Number): Value = when (value) {
        is Int -> getInt(value)
        is Float -> getFloat(value)
        is Long -> getLong(value)
        is Double -> getDouble(value)
        else -> unreachable("Unknown number: $value")
    }

    fun getConstant(value: Any?): Value? = when (value) {
        is Int -> getInt(value)
        is Float -> getFloat(value)
        is Long -> getLong(value)
        is Double -> getDouble(value)
        is String -> getString(value)
        else -> null
    }

    fun unwrapConstant(constant: Value?): Any? = when (constant) {
        is BoolConstant -> if (constant.value) 1 else 0
        is ByteConstant -> constant.value.toInt()
        is ShortConstant -> constant.value.toInt()
        is IntConstant -> constant.value
        is LongConstant -> constant.value
        is CharConstant -> constant.value.code
        is FloatConstant -> constant.value
        is DoubleConstant -> constant.value
        is StringConstant -> constant.value
        else -> null
    }
}
