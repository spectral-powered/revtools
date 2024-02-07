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

package org.spectralpowered.revtools.asm.builder.cfg.impl

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.TypeInsnNode
import org.spectralpowered.revtools.asm.ir.Method
import org.spectralpowered.revtools.asm.ir.value.Value
import org.spectralpowered.revtools.asm.type.Type
import org.spectralpowered.revtools.asm.type.TypeFactory
import org.spectralpowered.revtools.asm.type.parseDescOrNull
import org.spectralpowered.revtools.asm.helper.assert.unreachable
import java.util.*

private object TopType : Type() {
    override val name: String
        get() = "TOP"
    override val asmDesc: String
        get() = "T"
    override val isPrimitive: Boolean
        get() = false
    override val bitSize: Int
        get() = WORD

    override val isConcrete: Boolean
        get() = true

    override fun isSubtypeOf(other: Type, outerClassBehavior: Boolean) = false
}

private object UninitializedThisType : Type() {
    override val name: String
        get() = "this"
    override val asmDesc: String
        get() = "U"
    override val isPrimitive: Boolean
        get() = false
    override val bitSize: Int
        get() = WORD

    override val isConcrete: Boolean
        get() = true

    override fun isSubtypeOf(other: Type, outerClassBehavior: Boolean) = false
}

private fun parsePrimitiveType(tf: TypeFactory, opcode: Int) = when (opcode) {
    0 -> TopType
    1 -> tf.intType
    2 -> tf.floatType
    3 -> tf.doubleType
    4 -> tf.longType
    5 -> tf.nullType
    6 -> UninitializedThisType
    else -> unreachable("Unknown opcode in primitive type parsing: $opcode")
}

fun parseFrameDesc(tf: TypeFactory, desc: String): Type = when (desc.length) {
    1 -> when (desc[0]) {
        'V' -> tf.voidType
        'Z' -> tf.boolType
        'B' -> tf.byteType
        'C' -> tf.charType
        'S' -> tf.shortType
        'I' -> tf.intType
        'J' -> tf.longType
        'F' -> tf.floatType
        'D' -> tf.doubleType
        else -> unreachable("Unknown primitive type descriptor: $desc")
    }

    else -> when (desc[0]) {
        '[' -> parseDescOrNull(tf, desc.drop(1))!!.asArray
        else -> tf.getRefType(desc)
    }
}

private fun parseType(types: TypeFactory, any: Any): Type = when (any) {
    is String -> parseFrameDesc(types, any)
    is Int -> parsePrimitiveType(types, any)
    is LabelNode -> {
        val newNode: TypeInsnNode = any.run {
            var cur: AbstractInsnNode = this
            var typeInsnNode: TypeInsnNode?
            do {
                typeInsnNode = cur.next as? TypeInsnNode
                cur = cur.next
            } while (typeInsnNode == null)
            typeInsnNode
        }
        parseFrameDesc(types, newNode.desc)
    }

    else -> unreachable("Unexpected local type $any")
}

private fun List<*>?.parseLocals(types: TypeFactory): SortedMap<Int, Type> {
    if (this == null) return sortedMapOf()
    val result = mutableMapOf<Int, Type>()
    var index = 0
    for (any in this) {
        val type = parseType(types, any!!)
        result[index] = type
        when {
            type.isDWord -> index += 2
            else -> ++index
        }
    }
    return result.toSortedMap()
}

private fun List<*>?.parseStack(types: TypeFactory): SortedMap<Int, Type> {
    if (this == null) return sortedMapOf()
    val result = mutableMapOf<Int, Type>()
    for ((index, any) in this.withIndex()) {
        val type = parseType(types, any!!)
        result[index] = type
    }
    return result.toSortedMap()
}

internal data class FrameState(
    val types: TypeFactory,
    val method: Method,
    private val innerLocal: SortedMap<Int, Type>,
    private val innerStack: SortedMap<Int, Type>
) {
    val local: SortedMap<Int, Type> get() = innerLocal.filtered
    val stack: SortedMap<Int, Type> get() = innerStack.filtered

    private val SortedMap<Int, Type>.filtered: SortedMap<Int, Type>
        get() = this.filterValues { it !is TopType }.mapValues {
            when (it.value) {
                is UninitializedThisType -> types.getRefType(method.klass)
                else -> it.value
            }
        }.toSortedMap()

    companion object {
        fun parse(types: TypeFactory, method: Method, inst: FrameNode) = FrameState(
            types,
            method,
            inst.local.parseLocals(types),
            inst.stack.parseStack(types)
        )

        fun parse(types: TypeFactory, method: Method, locals: Map<Int, Value>, stack: List<Value>) = FrameState(
            types,
            method,
            locals.mapValuesTo(sortedMapOf()) { it.value.type },
            stack.withIndex().associateTo(sortedMapOf()) { it.index to it.value.type }
        )
    }

    fun appendFrame(inst: FrameNode): FrameState {
        val maxKey = this.innerLocal.keys.maxOrNull() ?: -1
        val lastType = innerLocal[maxKey]
        val insertKey = when {
            lastType == null -> 0
            lastType.isDWord -> maxKey + 2
            else -> maxKey + 1
        }
        val appendedLocals = inst.local.parseLocals(types)
        val newLocals = this.innerLocal.toSortedMap()
        for ((index, type) in appendedLocals) {
            newLocals[insertKey + index] = type
        }
        return copy(innerLocal = newLocals, innerStack = sortedMapOf())
    }

    fun dropFrame(inst: FrameNode): FrameState {
        val newLocals = sortedMapOf<Int, Type>()
        val maxSize = this.innerLocal.size - inst.local.size
        for ((index, type) in this.innerLocal) {
            if (newLocals.size == maxSize) break
            newLocals[index] = type
        }
        return copy(innerLocal = newLocals, innerStack = sortedMapOf())
    }

    fun copy(): FrameState = this.copy(innerStack = sortedMapOf())

    fun copy1(inst: FrameNode): FrameState = this.copy(innerStack = inst.stack.parseStack(types))
}
