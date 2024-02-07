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

@file:Suppress("unused")

package org.spectralpowered.revtools.asm.helper

import org.spectralpowered.revtools.asm.helper.assert.unreachable
import kotlin.reflect.KClass

fun Boolean.toInt(): Int = if (this) 1 else 0
fun Int.toBoolean(): Boolean = this != 0
fun Number.toBoolean(): Boolean = toInt().toBoolean()

fun Number.cast(type: KClass<*>): Any = when (type) {
    Byte::class -> toByte()
    Short::class -> toShort()
    Int::class -> toInt()
    Long::class -> toLong()
    Float::class -> toFloat()
    Double::class -> toDouble()
    else -> throw IllegalStateException("Unsupported number type")
}

inline fun <reified T> Number.cast() = cast(T::class) as T

operator fun Number.plus(other: Number): Number = when (this) {
    is Long -> this.toLong() + other.toLong()
    is Int -> this.toInt() + other.toInt()
    is Short -> this.toShort() + other.toShort()
    is Byte -> this.toByte() + other.toByte()
    is Double -> this.toDouble() + other.toDouble()
    is Float -> this.toFloat() + other.toFloat()
    else -> unreachable("Unknown numeric type")
}

operator fun Number.minus(other: Number): Number = when (this) {
    is Long -> this.toLong() - other.toLong()
    is Int -> this.toInt() - other.toInt()
    is Short -> this.toShort() - other.toShort()
    is Byte -> this.toByte() - other.toByte()
    is Double -> this.toDouble() - other.toDouble()
    is Float -> this.toFloat() - other.toFloat()
    else -> unreachable("Unknown numeric type")
}

operator fun Number.times(other: Number): Number = when (this) {
    is Long -> this.toLong() * other.toLong()
    is Int -> this.toInt() * other.toInt()
    is Short -> this.toShort() * other.toShort()
    is Byte -> this.toByte() * other.toByte()
    is Double -> this.toDouble() * other.toDouble()
    is Float -> this.toFloat() * other.toFloat()
    else -> unreachable("Unknown numeric type")
}

operator fun Number.div(other: Number): Number = when (this) {
    is Long -> this.toLong() / other.toLong()
    is Int -> this.toInt() / other.toInt()
    is Short -> this.toShort() / other.toShort()
    is Byte -> this.toByte() / other.toByte()
    is Double -> this.toDouble() / other.toDouble()
    is Float -> this.toFloat() / other.toFloat()
    else -> unreachable("Unknown numeric type")
}

operator fun Number.rem(other: Number): Number = when (this) {
    is Long -> this.toLong() % other.toLong()
    is Int -> this.toInt() % other.toInt()
    is Short -> this.toShort() % other.toShort()
    is Byte -> this.toByte() % other.toByte()
    is Double -> this.toDouble() % other.toDouble()
    is Float -> this.toFloat() % other.toFloat()
    else -> unreachable("Unknown numeric type")
}

operator fun Number.unaryMinus(): Number = when (this) {
    is Long -> this.toLong().unaryMinus()
    is Int -> this.toInt().unaryMinus()
    is Short -> this.toShort().unaryMinus()
    is Byte -> this.toByte().unaryMinus()
    is Double -> this.toDouble().unaryMinus()
    is Float -> this.toFloat().unaryMinus()
    else -> unreachable("Unknown numeric type")
}

operator fun Number.compareTo(other: Number): Int = when (this) {
    is Long -> this.toLong().compareTo(other.toLong())
    is Int -> this.toInt().compareTo(other.toInt())
    is Short -> this.toShort().compareTo(other.toShort())
    is Byte -> this.toByte().compareTo(other.toByte())
    is Double -> this.toDouble().compareTo(other.toDouble())
    is Float -> this.toFloat().compareTo(other.toFloat())
    else -> unreachable("Unknown numeric type")
}

infix fun Number.shl(bits: Int): Number = when (this) {
    is Long -> this.toLong().shl(bits)
    is Int -> this.toInt().shl(bits)
    is Short -> this.toShort().shl(bits)
    is Byte -> this.toByte().shl(bits)
    is Double -> this.toDouble().shl(bits)
    is Float -> this.toFloat().shl(bits)
    else -> unreachable("Unknown numeric type")
}

infix fun Number.shr(bits: Int): Number = when (this) {
    is Long -> this.toLong().shr(bits)
    is Int -> this.toInt().shr(bits)
    is Short -> this.toShort().shr(bits)
    is Byte -> this.toByte().shr(bits)
    is Double -> this.toDouble().shr(bits)
    is Float -> this.toFloat().shr(bits)
    else -> unreachable("Unknown numeric type")
}

infix fun Number.ushr(bits: Int): Number = when (this) {
    is Long -> this.toLong().ushr(bits)
    is Int -> this.toInt().ushr(bits)
    is Short -> this.toShort().ushr(bits)
    is Byte -> this.toByte().ushr(bits)
    is Double -> this.toDouble().ushr(bits)
    is Float -> this.toFloat().ushr(bits)
    else -> unreachable("Unknown numeric type")
}

infix fun Number.and(other: Number): Number = when (this) {
    is Long -> this.toLong() and other.toLong()
    is Int -> this.toInt() and other.toInt()
    is Short -> this.toShort() and other.toShort()
    is Byte -> this.toByte() and other.toByte()
    is Double -> this.toDouble() and other.toDouble()
    is Float -> this.toFloat() and other.toFloat()
    else -> unreachable("Unknown numeric type")
}

infix fun Number.or(other: Number): Number = when (this) {
    is Long -> this.toLong() or other.toLong()
    is Int -> this.toInt() or other.toInt()
    is Short -> this.toShort() or other.toShort()
    is Byte -> this.toByte() or other.toByte()
    is Double -> this.toDouble() or other.toDouble()
    is Float -> this.toFloat() or other.toFloat()
    else -> unreachable("Unknown numeric type")
}

infix fun Number.xor(other: Number): Number = when (this) {
    is Long -> this.toLong() xor other.toLong()
    is Int -> this.toInt() xor other.toInt()
    is Short -> this.toShort() xor other.toShort()
    is Byte -> this.toByte() xor other.toByte()
    is Double -> this.toDouble() xor other.toDouble()
    is Float -> this.toFloat() xor other.toFloat()
    else -> unreachable("Unknown numeric type")
}

fun minOf(vararg numbers: Number): Number {
    if (numbers.isEmpty()) throw IllegalStateException()
    var min = numbers.first()
    for (i in 1..numbers.lastIndex) {
        if (numbers[i] < min) min = numbers[i]
    }
    return min
}

fun maxOf(vararg numbers: Number): Number {
    if (numbers.isEmpty()) throw IllegalStateException()
    var max = numbers.first()
    for (i in 1..numbers.lastIndex) {
        if (numbers[i] > max) max = numbers[i]
    }
    return max
}

fun minOf(numbers: Collection<Number>): Number {
    if (numbers.isEmpty()) throw IllegalStateException()
    var min = numbers.first()
    for (num in numbers) {
        if (num < min) min = num
    }
    return min
}

fun maxOf(numbers: Collection<Number>): Number {
    if (numbers.isEmpty()) throw IllegalStateException()
    var max = numbers.first()
    for (num in numbers) {
        if (num > max) max = num
    }
    return max
}
