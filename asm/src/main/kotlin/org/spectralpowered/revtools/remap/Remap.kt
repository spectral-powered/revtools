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

package org.spectralpowered.revtools.remap

import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InnerClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode
import org.objectweb.asm.tree.TryCatchBlockNode
import org.objectweb.asm.tree.TypeInsnNode

fun ClassNode.remap(remapper: Remapper) {
    val origName = name
    name = remapper.mapType(origName)
    signature = remapper.mapSignature(signature, false)
    superName = remapper.mapType(superName)
    interfaces = interfaces?.map(remapper::mapType)
    sourceFile = "$name.java"

    val origOuterClass = outerClass
    outerClass = remapper.mapType(origOuterClass)

    if(outerMethod != null) {
        outerMethod = remapper.mapMethodName(origOuterClass, outerMethod, outerMethodDesc)
        outerMethodDesc = remapper.mapMethodDesc(outerMethodDesc)
    }

    innerClasses.forEach { innerClass ->
        innerClass.remap(remapper)
    }

    fields.forEach { field ->
        field.remap(remapper, origName)
    }

    methods.forEach { method ->
        method.remap(remapper, origName)
    }

    visibleAnnotations?.forEach { it.remap(remapper) }
    invisibleAnnotations?.forEach { it.remap(remapper) }
}

fun InnerClassNode.remap(remapper: Remapper) {
    name = remapper.mapType(name)
    outerName = remapper.mapType(outerName)
    innerName = remapper.mapType(innerName)
}

fun FieldNode.remap(remapper: Remapper, owner: String) {
    name = remapper.mapFieldName(owner, name, desc)
    desc = remapper.mapDesc(desc)
    signature = remapper.mapSignature(signature, true)
    value = remapper.mapValue(value)
    visibleAnnotations?.forEach { it.remap(remapper) }
    invisibleAnnotations?.forEach { it.remap(remapper) }
}

fun MethodNode.remap(remapper: Remapper, owner: String) {
    name = remapper.mapMethodName(owner, name, desc)
    desc = remapper.mapMethodDesc(desc)
    signature = remapper.mapSignature(signature, false)
    exceptions = exceptions.map(remapper::mapType)

    instructions.forEach { insn ->
        insn.remap(remapper)
    }

    tryCatchBlocks.forEach { tcb ->
        tcb.remap(remapper)
    }

    visibleAnnotations?.forEach { it.remap(remapper) }
    invisibleAnnotations?.forEach { it.remap(remapper) }
}

fun TryCatchBlockNode.remap(remapper: Remapper) {
    type = remapper.mapType(type)
}

fun AbstractInsnNode.remap(remapper: Remapper) {
    when(this) {
        is FieldInsnNode -> {
            val origOwner = owner
            owner = remapper.mapType(origOwner)
            name = remapper.mapFieldName(origOwner, name, desc)
            desc = remapper.mapDesc(desc)
        }

        is MethodInsnNode -> {
            val origOwner = owner
            owner = remapper.mapType(origOwner)
            name = remapper.mapMethodName(origOwner, name, desc)
            desc = remapper.mapMethodDesc(desc)
        }

        is TypeInsnNode -> desc = remapper.mapType(desc)
        is LdcInsnNode -> cst = remapper.mapValue(cst)
        is MultiANewArrayInsnNode -> desc = remapper.mapType(desc)
    }
}

fun AnnotationNode.remap(remapper: Remapper) {
    desc = remapper.mapDesc(desc)
}