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

import org.spectralpowered.revtools.asm.UnsupportedOperationException
import org.spectralpowered.revtools.asm.ir.Class
import org.spectralpowered.revtools.asm.ir.ConcreteClass

sealed class Reference : Type() {
    override val bitSize: Int
        get() = WORD

    override val isPrimitive get() = false
    override val isReference get() = true
}

open class ClassType(val klass: Class) : Reference() {
    override val name = klass.fullName

    override fun toString() = name
    override val asmDesc get() = "L${klass.fullName};"

    override fun hashCode() = klass.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ClassType
        return this.klass == other.klass
    }

    override val isConcrete: Boolean
        get() = klass is ConcreteClass

    override fun isSubtypeOf(other: Type, outerClassBehavior: Boolean): Boolean = when (other) {
        is ClassType -> this.klass.isInheritorOf(other.klass, outerClassBehavior)
        else -> false
    }
}

open class ArrayType(val component: Type) : Reference() {
    override val name = "$component[]"
    override fun toString() = name
    override val asmDesc get() = "[${component.asmDesc}"

    override fun hashCode() = component.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (this.javaClass != other?.javaClass) return false
        other as ArrayType
        return this.component == other.component
    }

    override val isConcrete: Boolean
        get() = component.isConcrete

    override fun isSubtypeOf(other: Type, outerClassBehavior: Boolean): Boolean = when (other) {
        this -> true
        is ArrayType -> when {
            this.component.isReference && other.component.isReference -> this.component.isSubtypeOf(
                other.component,
                outerClassBehavior
            )

            else -> false
        }

        is ClassType -> when (other.klass.fullName) {
            SystemTypeNames.objectClass -> true
            SystemTypeNames.cloneableClass -> true
            SystemTypeNames.serializableClass -> true
            else -> false
        }

        else -> false
    }
}

object NullType : Reference() {
    override val name = "null"

    override fun toString() = name
    override val asmDesc get() = throw UnsupportedOperationException("Called getAsmDesc on NullType")

    override fun hashCode() = name.hashCode()
    override fun equals(other: Any?): Boolean = this === other

    override val isConcrete: Boolean
        get() = true

    override fun isSubtypeOf(other: Type, outerClassBehavior: Boolean) = true
}
