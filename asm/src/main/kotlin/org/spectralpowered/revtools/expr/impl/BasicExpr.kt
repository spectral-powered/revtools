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

package org.spectralpowered.revtools.expr.impl

import org.objectweb.asm.tree.AbstractInsnNode
import org.spectralpowered.revtools.expr.ExprTree
import org.spectralpowered.revtools.toOpcodeString

open class BasicExpr(var insn: AbstractInsnNode, internal var index: Int, internal var size: Int) : Iterable<BasicExpr> {

    lateinit var tree: ExprTree internal set
    val method get() = tree.method

    val opcode get() = insn.opcode
    open val instruction get() = insn

    var previous: BasicExpr? = null
    var next: BasicExpr? = null

    var parent: BasicExpr? = null
    val children = ArrayDeque<BasicExpr>()

    override fun iterator(): Iterator<BasicExpr> {
        return children.iterator()
    }

    fun addChild(expr: BasicExpr) {
        expr.parent = this
        children.addFirst(expr)
    }

    override fun toString(): String {
        return insn.toOpcodeString()
    }
}