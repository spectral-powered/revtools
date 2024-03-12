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

package org.spectralpowered.revtools.asm.remap

import org.objectweb.asm.Type
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.*

fun ClassNode.remap(remapper: AsmRemapper) {
    val originalName = name
    name = remapper.mapType(originalName)
    signature = remapper.mapSignature(signature, false)
    superName = remapper.mapType(superName)
    interfaces = interfaces?.map(remapper::mapType)

    val originalOuterClass = outerClass
    outerClass = remapper.mapType(originalOuterClass)

    if (outerMethod != null) {
        outerMethod = remapper.mapMethodName(originalOuterClass, outerMethod, outerMethodDesc)
        outerMethodDesc = remapper.mapMethodDesc(outerMethodDesc)
    }

    for (innerClass in innerClasses) {
        innerClass.remap(remapper)
    }

    for (field in fields) {
        field.remap(remapper, originalName)
    }

    for (method in methods) {
        method.remap(remapper, originalName)
    }

    visibleAnnotations?.forEach { it.remap(remapper) }
    invisibleAnnotations?.forEach { it.remap(remapper) }
}

fun InnerClassNode.remap(remapper: Remapper) {
    name = remapper.mapType(name)
    outerName = remapper.mapType(outerName)
    innerName = remapper.mapType(innerName)
}

fun FieldNode.remap(remapper: AsmRemapper, owner: String) {
    name = remapper.mapFieldName(owner, name, desc)
    desc = remapper.mapDesc(desc)
    signature = remapper.mapSignature(signature, true)
    value = remapper.mapValue(value)
    visibleAnnotations?.forEach { it.remap(remapper) }
    invisibleAnnotations?.forEach { it.remap(remapper) }
}

fun MethodNode.remap(remapper: AsmRemapper, owner: String) {
    if (parameters == null) {
        parameters = List(Type.getArgumentTypes(desc).size) { ParameterNode(null, 0) }
    }

    for ((index, parameter) in parameters.withIndex()) {
        parameter.remap(remapper, owner, name, desc, index)
    }

    name = remapper.mapMethodName(owner, name, desc)
    desc = remapper.mapMethodDesc(desc)
    signature = remapper.mapSignature(signature, false)
    exceptions = exceptions.map(remapper::mapType)

    for (insn in instructions) {
        insn.remap(remapper)
    }

    for (tryCatch in tryCatchBlocks) {
        tryCatch.remap(remapper)
    }

    visibleAnnotations?.forEach { it.remap(remapper) }
    invisibleAnnotations?.forEach { it.remap(remapper) }
}

fun ParameterNode.remap(
    remapper: AsmRemapper,
    owner: String,
    methodName: String,
    desc: String,
    index: Int
) {
    name = remapper.mapArgumentName(owner, methodName, desc, index, name)
}

fun TryCatchBlockNode.remap(remapper: Remapper) {
    type = remapper.mapType(type)
}

fun AbstractInsnNode.remap(remapper: AsmRemapper) {
    when (this) {
        is FrameNode -> throw UnsupportedOperationException("SKIP_FRAMES and COMPUTE_FRAMES must be used")
        is FieldInsnNode -> {
            val originalOwner = owner
            owner = remapper.mapFieldOwner(originalOwner, name, desc)
            name = remapper.mapFieldName(originalOwner, name, desc)
            desc = remapper.mapDesc(desc)
        }

        is MethodInsnNode -> {
            val originalOwner = owner
            owner = remapper.mapMethodOwner(originalOwner, name, desc)
            name = remapper.mapMethodName(originalOwner, name, desc)
            desc = remapper.mapDesc(desc)
        }

        is InvokeDynamicInsnNode -> {
            name = remapper.mapInvokeDynamicMethodName(name, desc)
        }

        is TypeInsnNode -> desc = remapper.mapType(desc)
        is LdcInsnNode -> cst = remapper.mapValue(cst)
        is MultiANewArrayInsnNode -> desc = remapper.mapType(desc)
    }
}

fun AnnotationNode.remap(remapper: AsmRemapper) {
    desc = remapper.mapDesc(desc)
}