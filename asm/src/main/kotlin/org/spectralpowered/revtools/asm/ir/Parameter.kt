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

import org.objectweb.asm.tree.AnnotationNode
import org.spectralpowered.revtools.asm.ClassGroup
import org.spectralpowered.revtools.asm.type.Type

open class Parameter(
    group: ClassGroup,
    val index: Int,
    name: String,
    val type: Type,
    modifiers: Modifiers,
    val annotations: List<MethodParameterAnnotation>
) : Node(group, name, modifiers) {
    override val asmDesc = type.asmDesc

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Parameter) return false

        if (index != other.index) return false
        if (type != other.type) return false
        if (asmDesc != other.asmDesc) return false
        return annotations == other.annotations
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + type.hashCode()
        result = 31 * result + asmDesc.hashCode()
        return result
    }
}

class StubParameter(
    group: ClassGroup,
    index: Int,
    type: Type,
    modifiers: Modifiers,
    annotations: List<MethodParameterAnnotation>
) : Parameter(group, index, name = NAME, type, modifiers, annotations) {
    companion object {
        const val NAME = "stub"
    }
}

/**
 * @param arguments: argument is a pair of name to value where value could be one of java's primitive type wrapper
 * (Integer, Long, ...), String, Reference, Enum or Array
 */
data class MethodParameterAnnotation(
    val type: Type,
    val arguments: Map<String, Any>
) {
    companion object {
        fun get(annotationNode: AnnotationNode, group: ClassGroup): MethodParameterAnnotation {
            val fullName = getAnnotationFullName(annotationNode.desc)
            val type = group[fullName].asType

            val keys = annotationNode.values.orEmpty()
                .filterIndexed { index, _ -> index.mod(2) == 0 }
                .filterIsInstance<String>()
            val values = annotationNode.values.orEmpty()
                .filterIndexed { index, _ -> index.mod(2) == 1 }

            return MethodParameterAnnotation(type, (keys zip values).toMap())
        }

        private fun getAnnotationFullName(desc: String): String {
            return desc.removePrefix("L").removeSuffix(";")
        }
    }
}
