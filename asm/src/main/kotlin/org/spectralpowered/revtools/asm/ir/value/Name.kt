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

package org.spectralpowered.revtools.asm.ir.value

import org.spectralpowered.revtools.asm.ir.BasicBlock
import org.spectralpowered.revtools.asm.ir.Method
import org.spectralpowered.revtools.asm.ir.MethodBody

sealed class Name {
    internal var index: Int = -1

    abstract fun clone(): Name
    override fun hashCode() = System.identityHashCode(this)
    override fun equals(other: Any?) = this === other
}

class StringName(val name: String) : Name() {
    override fun clone() = StringName(name)
    override fun toString(): String {
        val suffix = if (index == -1) "" else "$index"
        return "%$name$suffix"
    }
}

class Slot : Name() {
    override fun clone() = Slot()
    override fun toString(): String {
        return if (index == -1) "NO_SLOT_FOR${System.identityHashCode(this)}" else "%$index"
    }
}

class BlockName(val name: String) : Name() {
    override fun clone() = BlockName(name)
    override fun toString(): String {
        val suffix = if (index == -1) "" else "$index"
        return "%$name$suffix"
    }
}

data class ConstantName(val name: String) : Name() {
    override fun clone() = ConstantName(name)
    override fun toString() = name
}

class UndefinedName : Name() {
    override fun clone() = this
    override fun toString(): String {
        return if (index == -1) "UNDEFINED_${System.identityHashCode(this)}" else "undefined$index"
    }
}

class SlotTracker(private val methodBody: MethodBody) {
    private val blocks = hashMapOf<String, Int>()
    private val strings = hashMapOf<String, Int>()
    private var slots: Int = 0
    private var undefinedNames: Int = 0

    @Suppress("unused")
    constructor(method: Method) : this(method.body)

    fun addBlock(block: BasicBlock) {
        val name = block.name
        name.index = blocks.getOrDefault(name.name, 0)
        blocks[name.name] = name.index + 1
    }

    @Suppress("UNUSED_PARAMETER")
    fun removeBlock(block: BasicBlock) {
        // nothing
    }

    fun addValue(value: Value) {
        val name = value.name
        name.index = when (name) {
            is Slot -> slots++
            is StringName -> {
                val result = strings.getOrDefault(name.name, 0)
                strings[name.name] = result + 1
                result
            }
            is UndefinedName -> undefinedNames++
            else -> -1
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun removeValue(value: Value) {
        // nothing
    }

    fun rerun() {
        strings.clear()
        slots = 0
        blocks.clear()
        undefinedNames = 0
        for (bb in methodBody) {
            addBlock(bb)
            for (inst in bb) {
                for (value in inst.operands.plus(inst.get())) {
                    val name = value.name
                    name.index = when (name) {
                        is Slot -> slots++
                        is UndefinedName -> undefinedNames++
                        is StringName -> {
                            val result = strings.getOrDefault(name.name, 0)
                            strings[name.name] = result + 1
                            result
                        }
                        else -> -1
                    }
                }
            }
        }
    }
}

@Suppress("unused")
class NameMapper(val method: Method) {
    private val stringToName = hashMapOf<String, Name>()
    private val nameToValue = hashMapOf<Name, Value>()
    private val nameToBlock = hashMapOf<BlockName, BasicBlock>()

    init {
        init()
    }

    fun getBlock(name: String) = nameToBlock
        .filter { it.key.toString() == name }
        .map { it.value }
        .firstOrNull()

    fun getBlock(name: BlockName) = nameToBlock[name]

    fun getValue(name: String) = nameToValue[stringToName[name]]

    fun getValue(name: Name) = nameToValue[name]

    private fun init() {
        for (bb in method.body) {
            nameToBlock[bb.name] = bb
            for (inst in bb) {
                for (value in inst.operands.plus(inst.get())) {
                    val name = value.name
                    if (value !is Constant) {
                        nameToValue[name] = value
                        stringToName[name.toString()] = name
                    }
                }
            }
        }
    }
}

class NameMapperContext {
    private val mappers = mutableMapOf<Method, NameMapper>()

    fun getMapper(method: Method) = mappers.getOrPut(method) { NameMapper(method) }

    fun clear() {
        mappers.clear()
    }
}

val Method.nameMapper get() = NameMapper(this)
