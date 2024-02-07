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

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.FrameNode
import org.spectralpowered.revtools.asm.InvalidOpcodeException
import org.spectralpowered.revtools.asm.InvalidStateException
import java.util.regex.Pattern

val FrameNode.frameType get() = FrameNodeHelper.getFrameType(this)

@Suppress("RecursivePropertyAccessor")
val Type.internalDesc: String
    get() = when {
        this.isPrimitive -> this.asmDesc
        this is ClassType -> this.klass.fullName
        this is ArrayType -> "[${(component as? ClassType)?.asmDesc ?: component.internalDesc}"
        else -> throw InvalidStateException("Unknown type ${this.name}")
    }

@Suppress("RecursivePropertyAccessor")
val Type.typeFactory: TypeFactory?
    get() = when (this) {
        is PrimitiveType -> null
        is ClassType -> klass.group.type
        is ArrayType -> component.typeFactory
        else -> null
    }

fun commonSupertype(types: Set<Type>): Type? = when {
    NullType in types -> {
        val filtered = types.filterNotTo(mutableSetOf()) { it == NullType }
        when {
            filtered.isEmpty() -> NullType
            else -> commonSupertype(filtered)
        }
    }

    types.size == 1 -> types.first()
    types.all { it is Integer } -> types.maxByOrNull { (it as Integer).width }
    types.all { it is ClassType } -> {
        val classes = types.map { it as ClassType }
        val tf = classes.firstNotNullOf { it.typeFactory }
        var result = tf.objectType
        for (i in classes.indices) {
            val isAncestor = classes.fold(true) { acc, klass ->
                acc && classes[i].klass.isAncestorOf(klass.klass)
            }

            if (isAncestor) {
                result = classes[i]
            }
        }
        result
    }

    types.all { it is Reference } -> {
        val tf = types.firstNotNullOfOrNull { it.typeFactory }
        when {
            types.any { it is ClassType } -> tf?.objectType
            types.map { it as ArrayType }.mapTo(mutableSetOf()) { it.component }.size == 1 -> types.first()
            types.all { it is ArrayType } -> {
                val components = types.mapTo(mutableSetOf()) { (it as ArrayType).component }
                when (val merged = commonSupertype(components)) {
                    null -> tf?.objectType
                    else -> merged.asArray
                }
            }

            else -> tf?.objectType
        }
    }

    else -> null
}

fun parseDescOrNull(tf: TypeFactory, desc: String): Type? = when (desc[0]) {
    'V' -> tf.voidType
    'Z' -> tf.boolType
    'B' -> tf.byteType
    'C' -> tf.charType
    'S' -> tf.shortType
    'I' -> tf.intType
    'J' -> tf.longType
    'F' -> tf.floatType
    'D' -> tf.doubleType
    'L' -> when {
        desc.last() != ';' -> null
        else -> tf.getRefType(desc.drop(1).dropLast(1))
    }

    '[' -> parseDescOrNull(tf, desc.drop(1))?.asArray
    else -> null
}

fun parsePrimaryType(tf: TypeFactory, opcode: Int): Type = when (opcode) {
    Opcodes.T_CHAR -> tf.charType
    Opcodes.T_BOOLEAN -> tf.boolType
    Opcodes.T_BYTE -> tf.byteType
    Opcodes.T_DOUBLE -> tf.doubleType
    Opcodes.T_FLOAT -> tf.floatType
    Opcodes.T_INT -> tf.intType
    Opcodes.T_LONG -> tf.longType
    Opcodes.T_SHORT -> tf.shortType
    else -> throw InvalidOpcodeException("PrimaryType opcode $opcode")
}

fun primaryTypeToInt(type: Type): Int = when (type) {
    is CharType -> Opcodes.T_CHAR
    is BoolType -> Opcodes.T_BOOLEAN
    is ByteType -> Opcodes.T_BYTE
    is DoubleType -> Opcodes.T_DOUBLE
    is FloatType -> Opcodes.T_FLOAT
    is IntType -> Opcodes.T_INT
    is LongType -> Opcodes.T_LONG
    is ShortType -> Opcodes.T_SHORT
    else -> throw InvalidOpcodeException("${type.name} is not primary type")
}

private val typePattern = Pattern.compile("\\[*(V|Z|B|C|S|I|J|F|D|(L[^;]+;))")
fun parseMethodDesc(tf: TypeFactory, desc: String): Pair<List<Type>, Type> {
    val args = mutableListOf<Type>()
    val matcher = typePattern.matcher(desc)
    while (matcher.find()) {
        args.add(parseDescOrNull(tf, matcher.group(0))!!)
    }
    val returnType = args.last()
    return Pair(args.dropLast(1), returnType)
}

private fun parseNamedType(tf: TypeFactory, name: String): Type? = when (name) {
    "null" -> tf.nullType
    "void" -> tf.voidType
    "bool" -> tf.boolType
    "byte" -> tf.byteType
    "short" -> tf.shortType
    "long" -> tf.longType
    "char" -> tf.charType
    "int" -> tf.intType
    "float" -> tf.floatType
    "double" -> tf.doubleType
    else -> null
}

@Suppress("unused")
fun parseStringToType(tf: TypeFactory, name: String): Type {
    var arrCount = 0
    val end = name.dropLastWhile {
        if (it == '[') ++arrCount
        it == '[' || it == ']'
    }
    var subtype = parseNamedType(tf, end) ?: tf.getRefType(end)
    while (arrCount > 0) {
        --arrCount
        subtype = tf.getArrayType(subtype)
    }
    return subtype
}

@Suppress("unused")
@Deprecated("Unused")
val Type.expandedBitSize
    get() = when (this) {
        is ClassType -> klass.fields.fold(0) { acc, field -> acc + field.type.bitSize }
        else -> bitSize
    }
