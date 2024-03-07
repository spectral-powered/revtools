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

package org.spectralpowered.revtools.expr

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import org.spectralpowered.revtools.expr.impl.BasicExpr
import org.spectralpowered.revtools.util.stackMetadata
import java.util.concurrent.atomic.AtomicInteger

/**
 * `ExprTree` is a class that represents an expression tree. It implements the `Iterable` interface for `BasicExpr` objects.
 * It has a private constructor that takes a `MethodNode` as a parameter.
 *
 * @property exprs A mutable list of `BasicExpr` that represents the expressions in this tree.
 */
class ExprTree private constructor(private val exprs: MutableList<BasicExpr> = mutableListOf()) : Iterable<BasicExpr> {

    lateinit var method: MethodNode internal set

    init {
        exprs.forEach { it.tree = this }
    }

    /**
     * Overrides the `iterator` function from the `Iterable` interface.
     * It returns an iterator for the `exprs` list.
     *
     * @return An iterator for the `exprs` list.
     */
    override fun iterator(): Iterator<BasicExpr> {
        return exprs.iterator()
    }

    fun accept(visitor: ExprTreeVisitor) {
        visitor.visitStart(this)
        for(expr in this) {
            accept(visitor, expr)
        }
        visitor.visitEnd(this)
    }

    private fun accept(visitor: ExprTreeVisitor, parent: BasicExpr) {
        visitor.visitExpr(parent)
        parent.forEach { expr -> accept(visitor, expr) }
    }

    private class Builder {

        fun build(method: MethodNode): ExprTree {
            val stack = arrayListOf<AbstractInsnNode>().also { it.addAll(method.instructions) }
            stack.reverse()
            val exprList = arrayListOf<BasicExpr>()
            val stackIdx = AtomicInteger(0)
            stack.forEach { insn ->
                val expr = createExpr(insn, stackIdx.getAndIncrement(), insn.stackMetadata.pops)
                exprList.add(expr)
            }
            val exprs = ArrayDeque<BasicExpr>()
            val idx = AtomicInteger(0)
            var prev: BasicExpr? = null
            while(idx.get() < exprList.size) {
                val startIdx = idx.get()
                handleExpr(exprList, -1, idx)
                idx.incrementAndGet()
                val expr = exprList[startIdx]
                if(prev != null) {
                    expr.next = prev
                    prev.previous = expr
                }
                exprs.addFirst(expr)
                prev = expr
            }
            return ExprTree(exprs).also { it.method = method }
        }

        @Suppress("KotlinConstantConditions")
        private fun handleExpr(exprList: MutableList<BasicExpr>, parentIdx: Int, idx: AtomicInteger): Int {
            var consume = 0
            val expr = if(idx.get() >= exprList.size) null else exprList[idx.get()]
            if(expr != null) {
                if(parentIdx != -1) {
                    exprList[parentIdx].addChild(expr)
                }
                if(expr.opcode == GOTO) {
                    if(expr.parent != null) {
                        consume = expr.parent!!.size
                    }
                } else {
                    consume = when(expr.opcode) {
                        POP2 -> 1
                        DUP_X1 -> 1
                        DUP2, DUP_X2, DUP2_X1, DUP2_X2 -> if(exprList[idx.get() + 1].insn.isWide()) 1 else 2
                        else -> consume
                    }
                    var prev: BasicExpr? = null
                    var i = 0
                    while(i < expr.size) {
                        idx.incrementAndGet()
                        val child = if(exprList.size > idx.get()) exprList[idx.get()] else null
                        if(child != null && prev != null) {
                            child.next = prev
                            prev.previous = child
                        }
                        i += handleExpr(exprList, expr.index, idx)
                        prev = child
                        i++
                    }
                }
            }
            return consume
        }

        private fun createExpr(insn: AbstractInsnNode, index: Int, pops: Int): BasicExpr = when(insn.opcode) {
            else -> BasicExpr(insn, index, pops)
        }

        private fun AbstractInsnNode.isWide(): Boolean = when(opcode) {
            LCONST_0, LCONST_1, DCONST_0, DCONST_1,
            I2L, F2L, D2L,
            L2D, F2D, I2D,
            LADD, LSUB, LMUL, LDIV,
            DADD, DSUB, DMUL, DDIV,
            LOR, LAND, LREM, LNEG, LSHL, LSHR,
            LLOAD, DLOAD,
            LSTORE, DSTORE -> true
            GETFIELD, GETSTATIC -> {
                this as FieldInsnNode
                desc == "J" || desc == "D"
            }
            INVOKESTATIC, INVOKEVIRTUAL, INVOKEDYNAMIC -> {
                this as MethodInsnNode
                desc.endsWith(")J") || desc.endsWith(")D")
            }
            LDC -> {
                this as LdcInsnNode
                cst != null && (cst is Long || cst is Double)
            }
            else -> false
        }
    }

    companion object {
        /**
         * A companion object that provides a static `build` function.
         * This function creates a new `Builder` instance with the provided `MethodNode` and builds an `ExprTree` from it.
         *
         * @param method The `MethodNode` instance that the `Builder` will use to create an `ExprTree`.
         * @return An instance of `ExprTree` built from the provided `MethodNode`.
         */
        fun build(method: MethodNode) = Builder().build(method)
    }
}