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

import org.spectralpowered.revtools.asm.helper.assert.unreachable

@Suppress("unused")
interface TypeVisitor {
    val types: TypeFactory

    fun visit(type: Type): Type = when (type) {
        is VoidType -> visitVoid(type)
        is PrimitiveType -> visitPrimary(type)
        is Reference -> visitReference(type)
        else -> unreachable("Unknown type: $type")
    }

    fun visitVoid(type: VoidType): Type = type

    fun visitPrimary(type: PrimitiveType) = when (type) {
        is Integer -> visitIntegral(type)
        is Real -> visitReal(type)
    }

    fun visitIntegral(type: Integer) = when (type) {
        is BoolType -> visitBool(type)
        is ByteType -> visitByte(type)
        is ShortType -> visitShort(type)
        is IntType -> visitInt(type)
        is LongType -> visitLong(type)
        is CharType -> visitChar(type)
    }

    fun visitBool(type: BoolType): Type = type
    fun visitByte(type: ByteType): Type = type
    fun visitShort(type: ShortType): Type = type
    fun visitChar(type: CharType): Type = type
    fun visitInt(type: IntType): Type = type
    fun visitLong(type: LongType): Type = type

    fun visitReal(type: Real) = when (type) {
        is FloatType -> visitFloat(type)
        is DoubleType -> visitDouble(type)
    }

    fun visitFloat(type: FloatType): Type = type
    fun visitDouble(type: DoubleType): Type = type

    fun visitReference(type: Reference): Type = when (type) {
        is NullType -> visitNull(type)
        is ClassType -> visitClass(type)
        is ArrayType -> visitArray(type)
    }

    fun visitNull(type: NullType): Type = type
    fun visitClass(type: ClassType): Type = type
    fun visitArray(type: ArrayType): Type = type
}
