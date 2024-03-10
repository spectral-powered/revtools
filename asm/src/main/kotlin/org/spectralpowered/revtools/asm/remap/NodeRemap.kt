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
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.InnerClassNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode
import org.objectweb.asm.tree.ParameterNode
import org.objectweb.asm.tree.TryCatchBlockNode
import org.objectweb.asm.tree.TypeInsnNode

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
}

fun ParameterNode.remap(
    remapper: AsmRemapper,
    owner: String,
    methodName: String,
    desc: String,
    index: Int
) {

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
            desc = remapper.mapMethodDesc(desc)
        }

        is InvokeDynamicInsnNode -> {
            name = remapper.mapInvokeDynamicMethodName(name, desc)
        }

        is TypeInsnNode -> desc = remapper.mapType(desc)
        is LdcInsnNode -> cst = remapper.mapValue(cst)
        is MultiANewArrayInsnNode -> desc = remapper.mapType(desc)
    }
}