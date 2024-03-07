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

open class E0Expr(insn: AbstractInsnNode, index: Int, size: Int) : BasicExpr(insn, index, size) {
    internal fun op(index: Int) = children[index]
    internal fun op(index: Int, value: BasicExpr) {
        children[index] = value
    }
}

open class E1Expr(insn: AbstractInsnNode, index: Int, size: Int) : E0Expr(insn, index, size) {
    var op1
        get() = op(0)
        set(value) { op(0, value) }
}

open class E2Expr(insn: AbstractInsnNode, index: Int, size: Int) : E1Expr(insn, index, size) {
    var op2
        get() = op(1)
        set(value) { op(1, value) }
}

open class E3Expr(insn: AbstractInsnNode, index: Int, size: Int) : E2Expr(insn, index, size) {
    var op3
        get() = op(2)
        set(value) { op(2, value) }
}

open class EnExpr(insn: AbstractInsnNode, index: Int, size: Int) : E3Expr(insn, index, size) {
    val ops get() = children
}