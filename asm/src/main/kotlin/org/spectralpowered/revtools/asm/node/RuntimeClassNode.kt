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

import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Opcodes.V1_8
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.spectralpowered.revtools.asm.util.toAsmName
import java.lang.reflect.Field
import java.lang.reflect.Method

class RuntimeClassNode(clazz: Class<*>) : ClassNode(ASM9) {

    init {
        version = V1_8
        access = clazz.modifiers
        name = clazz.name.toAsmName()
        superName = clazz.superclass?.name?.toAsmName()
        interfaces = clazz.interfaces.map { it.name.toAsmName() }
        methods = clazz.declaredMethods.map { it.toMethodNode() }
        fields = clazz.declaredFields.map { it.toFieldNode() }
        isRuntime = true
    }

    private fun Method.toMethodNode() = MethodNode(
            ASM9,
            modifiers,
            name,
            Type.getMethodDescriptor(this),
            signature,
            exceptionTypes.map { it.name.toAsmName() }.toTypedArray().let { if(it.isEmpty()) null else it }
    )

    private fun Field.toFieldNode() = FieldNode(
            ASM9,
            modifiers,
            name,
            Type.getDescriptor(this.type),
            signature,
            null
    )
}