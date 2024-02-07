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

package org.spectralpowered.revtools.asm.ir

import org.objectweb.asm.tree.FieldNode
import org.spectralpowered.revtools.asm.ClassGroup
import org.spectralpowered.revtools.asm.ir.value.Value
import org.spectralpowered.revtools.asm.type.Type
import org.spectralpowered.revtools.asm.type.parseDescOrNull

class Field : Node {
    val klass: Class
    internal val fn: FieldNode
    val type: Type
    var defaultValue: Value?

    constructor(group: ClassGroup, klass: Class, fn: FieldNode) : super(group, fn.name, Modifiers(fn.access)) {
        this.fn = fn
        this.klass = klass
        this.type = parseDescOrNull(group.type, fn.desc)!!
        this.defaultValue = group.value.getConstant(fn.value)
    }

    constructor(group: ClassGroup, klass: Class, name: String, type: Type, modifiers: Modifiers = Modifiers(0)) :
            super(group, name, modifiers) {
        this.fn = FieldNode(modifiers.value, name, type.asmDesc, null, null)
        this.klass = klass
        this.type = type
        this.defaultValue = null
    }

    override val asmDesc
        get() = type.asmDesc

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Field

        if (klass != other.klass) return false
        return type == other.type
    }

    override fun hashCode(): Int {
        var result = fn.hashCode()
        result = 31 * result + klass.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    override fun toString() = "${klass.fullName}.$name: $type"
}
