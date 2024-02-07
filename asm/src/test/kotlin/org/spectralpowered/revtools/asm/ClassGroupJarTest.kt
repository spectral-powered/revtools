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

package org.spectralpowered.revtools.asm

import io.kotest.core.spec.style.FunSpec
import org.spectralpowered.revtools.asm.container.JarContainer
import org.spectralpowered.revtools.asm.ir.Method
import org.spectralpowered.revtools.asm.visitor.MethodVisitor
import org.spectralpowered.revtools.asm.visitor.executePipeline
import java.nio.file.Paths

class ClassGroupJarTest : FunSpec({

    test("Load gamepack jar into class group.") {
        val container = JarContainer(Paths.get("../build/deob/gamepack.jar"))
        val group = ClassGroup()
        group.initialize(container)
        group.classes.filter { it.fullName.startsWith("org/") }.forEach { cls ->
            group.ignoreClass(cls)
        }
        group.classes.forEach { println("${it.fullName}") }

        executePipeline(group, Package.defaultPackage) {
            +MethodBuilder(group)
        }

        val clientCls = group["client"]
        val initMethod = clientCls.getMethod("init", "()V")

        println(initMethod.body)
    }

})

private class MethodBuilder(
    override val group: ClassGroup
) : MethodVisitor {
    override fun cleanup() {}

    override fun visit(method: Method) {
        method.body
    }
}