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

package org.spectralpowered.revtools.asm.util

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TryCatchBlockNode
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceMethodVisitor
import java.io.PrintWriter
import java.io.StringWriter

private val printer = Textifier()
private val mp = TraceMethodVisitor(printer)

fun ClassNode.print() = buildString {
    appendLine("Class $name")
    for (mn in methods) {
        appendLine(mn.print())
    }
}

fun MethodNode.print() = buildString {
    appendLine(name)
    for (insn in instructions) {
        append(insn.print())
    }
    for (insn in tryCatchBlocks) {
        append(insn.print())
    }
}

fun AbstractInsnNode.print(): String {
    this.accept(mp)
    val sw = StringWriter()
    printer.print(PrintWriter(sw))
    printer.getText().clear()
    return sw.toString()
}

fun TryCatchBlockNode.print() = buildString {
    append("${start.print().dropLast(1)} ")
    append("${end.print().dropLast(1)} ")
    append("${handler.print().dropLast(1)} ")
    appendLine(type ?: "java/lang/Throwable")
}