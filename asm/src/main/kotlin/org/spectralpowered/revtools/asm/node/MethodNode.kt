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

package org.spectralpowered.revtools.asm.node

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.spectralpowered.revtools.asm.MemberDesc
import org.spectralpowered.revtools.asm.MemberRef
import org.spectralpowered.revtools.asm.util.field

fun MethodNode.init(cls: ClassNode) {
    this.cls = cls
}

var MethodNode.cls: ClassNode by field()
val MethodNode.pool get() = cls.pool

val MethodNode.memberDesc get() = MemberDesc(name, desc)
val MethodNode.memberRef get() = MemberRef(cls, this)

fun MethodNode.isPublic() = (access and ACC_PUBLIC) != 0
fun MethodNode.isStatic() = (access and ACC_STATIC) != 0
fun MethodNode.isAbstract() = (access and ACC_ABSTRACT) != 0
fun MethodNode.isInterface() = (access and ACC_INTERFACE) != 0

fun MethodNode.removeArg(argIndex: Int) {
    val type = Type.getType(desc)
    val argType = type.argumentTypes[argIndex]
    val argTypes = type.argumentTypes.filterIndexed { index, _ -> index != argIndex }.toTypedArray()
    desc = Type.getMethodDescriptor(type.returnType, *argTypes)

    if (signature != null) {
        throw UnsupportedOperationException("Signatures unsupported")
    }

    parameters?.removeAt(argIndex)

    // remove annotations
    if (visibleAnnotableParameterCount != 0) {
        throw UnsupportedOperationException("Non-zero visibleAnnotableParameterCount unsupported")
    }

    if (visibleParameterAnnotations != null) {
        visibleParameterAnnotations =
            visibleParameterAnnotations.filterIndexed { index, _ -> index != argIndex }.toTypedArray()
    }

    if (invisibleAnnotableParameterCount != 0) {
        throw UnsupportedOperationException("Non-zero invisibleAnnotableParameterCount unsupported")
    }

    if (invisibleParameterAnnotations != null) {
        invisibleParameterAnnotations =
            invisibleParameterAnnotations.filterIndexed { index, _ -> index != argIndex }.toTypedArray()
    }

    // remap locals
    val localIndex = localIndex(access, argTypes, argIndex)
    val newLocalIndex = maxLocals - argType.size

    if (localVariables != null) {
        for (v in localVariables) {
            v.index = remap(v.index, argType, localIndex, newLocalIndex)
        }
    }

    if (visibleLocalVariableAnnotations != null) {
        for (annotation in visibleLocalVariableAnnotations) {
            annotation.index = remapAll(annotation.index, argType, localIndex, newLocalIndex)
        }
    }

    if (invisibleLocalVariableAnnotations != null) {
        for (annotation in invisibleLocalVariableAnnotations) {
            annotation.index = remapAll(annotation.index, argType, localIndex, newLocalIndex)
        }
    }

    var newLocalIndexUsed = false

    for (insn in instructions) {
        when (insn) {
            is VarInsnNode -> {
                insn.`var` = remap(insn.`var`, argType, localIndex, newLocalIndex)

                if (insn.`var` == newLocalIndex) {
                    newLocalIndexUsed = true
                }
            }

            is IincInsnNode -> {
                insn.`var` = remap(insn.`var`, argType, localIndex, newLocalIndex)

                if (insn.`var` == newLocalIndex) {
                    newLocalIndexUsed = true
                }
            }

            is FrameNode -> throw UnsupportedOperationException("SKIP_FRAMES and COMPUTE_FRAMES must be used")
        }
    }

    if (newLocalIndexUsed) {
        return
    }

    maxLocals -= argType.size

    if (localVariables != null) {
        localVariables.removeIf { it.index == newLocalIndex }
    }

    if (visibleLocalVariableAnnotations != null) {
        visibleLocalVariableAnnotations.removeIf { newLocalIndex in it.index }
    }

    if (invisibleLocalVariableAnnotations != null) {
        invisibleLocalVariableAnnotations.removeIf { newLocalIndex in it.index }
    }
}

private fun localIndex(access: Int, argTypes: Array<Type>, argIndex: Int): Int {
    var localIndex = 0
    if (access and ACC_STATIC == 0) {
        localIndex++
    }
    for (i in 0 until argIndex) {
        localIndex += argTypes[i].size
    }
    return localIndex
}

private fun remap(i: Int, argType: Type, localIndex: Int, newLocalIndex: Int): Int {
    return when {
        i > localIndex -> i - argType.size
        i == localIndex -> newLocalIndex
        else -> i
    }
}

private fun remapAll(indexes: List<Int>, argType: Type, localIndex: Int, newLocalIndex: Int): MutableList<Int> {
    return indexes.mapTo(mutableListOf()) { remap(it, argType, localIndex, newLocalIndex) }
}
