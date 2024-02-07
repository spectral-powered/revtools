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

package org.spectralpowered.revtools.asm.builder.asm

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InnerClassNode
import org.spectralpowered.revtools.asm.ClassGroup
import org.spectralpowered.revtools.asm.ir.Class
import org.spectralpowered.revtools.asm.ir.Field
import org.spectralpowered.revtools.asm.ir.Method
import org.spectralpowered.revtools.asm.visitor.ClassVisitor

class ClassBuilder(override val group: ClassGroup, val `class`: Class) : ClassVisitor {
    override fun cleanup() {}

    override fun visit(klass: Class) {
        val cn = klass.cn
        cn.access = klass.modifiers.value
        cn.superName = klass.superClass?.fullName
        cn.interfaces.clear()
        cn.interfaces.addAll(klass.interfaces.map { it.fullName })
        cn.outerClass = klass.outerClass?.fullName
        cn.outerMethod = klass.outerMethod?.name
        cn.outerMethodDesc = klass.outerMethod?.desc?.asmDesc
        cn.innerClasses.clear()
        cn.innerClasses.addAll(klass.innerClasses.map { (klass, modifiers) ->
            InnerClassNode(klass.fullName, klass.outerClass?.fullName, klass.name, modifiers.value)
        })
        cn.fields = klass.fields.map { it.fn }
        cn.methods = klass.allMethods.map { it.mn }
        super.visit(klass)
    }

    override fun visitMethod(method: Method) {
        AsmBuilder(group, method).build()
        // because sometimes ASM is not able to process kotlin-generated signatures
        method.mn.signature = null
    }

    override fun visitField(field: Field) {
        field.fn.value = group.value.unwrapConstant(field.defaultValue)
        // because sometimes ASM is not able to process kotlin-generated signatures
        field.fn.signature = null
    }

    fun build(): ClassNode {
        visit(`class`)
        // because sometimes ASM is not able to process kotlin-generated signatures
        `class`.cn.signature = null
        return `class`.cn
    }

    operator fun invoke(): ClassNode = build()
}
