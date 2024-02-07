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

package org.spectralpowered.revtools.deobfuscator.transformer

import com.google.common.collect.MultimapBuilder
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.Type.INT_TYPE
import org.objectweb.asm.Type.LONG_TYPE
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Interpreter
import org.objectweb.asm.tree.analysis.SourceInterpreter
import org.objectweb.asm.tree.analysis.SourceValue
import org.objectweb.asm.tree.analysis.Value
import org.spectralpowered.revtools.deobfuscator.Transformer
import org.spectralpowered.revtools.deobfuscator.asm.tree.ClassGroup
import org.spectralpowered.revtools.deobfuscator.asm.tree.cls
import java.math.BigInteger
import java.util.Collections
import java.util.TreeMap

class MultiplierRemover : Transformer {

    override fun run(group: ClassGroup) {
        val decoders = MultiplierSolver(group).computeDecoders()
        group.removeMultipliers(decoders)
    }

    private class MultiplierSolver(private val group: ClassGroup) {

        private val decoders = MultimapBuilder.hashKeys().arrayListValues().build<String, Number>()
        private val depDecoders = MultimapBuilder.hashKeys().arrayListValues().build<String, Pair<String, Number>>()
        private val depEncoders = MultimapBuilder.hashKeys().arrayListValues().build<String, Pair<String, Number>>()

        fun computeDecoders(): Map<String, Number> {
            val analyzer = Analyzer(ExprInterpreter())
            for(cls in group.classes) {
                for(method in cls.methods) {
                    findDupDecoders(method)
                    analyzer.analyze(cls.name, method)
                }
            }

            return unfold()
        }

        private fun findDupDecoders(method: MethodNode) {
            for(insn in method.instructions) {
                val ldc = insn as? LdcInsnNode ?: continue
                if(ldc.cst !is Int && ldc.cst !is Long) continue
                val put = ldc.previous as? FieldInsnNode ?: continue
                val dup = put.previous ?: continue
                val mul = ldc.next ?: continue
                if(mul.opcode !in listOf(IMUL, LMUL)) continue
                if (
                    (put.opcode == PUTFIELD  && put.desc == INT_TYPE.descriptor  && dup.opcode == DUP_X1) ||
                    (put.opcode == PUTFIELD  && put.desc == LONG_TYPE.descriptor && dup.opcode == DUP2_X1) ||
                    (put.opcode == PUTSTATIC && put.desc == INT_TYPE.descriptor  && dup.opcode == DUP) ||
                    (put.opcode == PUTSTATIC && put.desc == LONG_TYPE.descriptor && dup.opcode == DUP2)
                ) {
                    val fieldString = "${put.owner}.${put.name}"
                    decoders.put(fieldString, ldc.cst as Number)
                }
            }
        }

        private fun unfold(): Map<String, Number> {
            val results = TreeMap<String, Number>()
            decoders.asMap().mapValuesTo(results) { d ->
                checkNotNull(d.value.maxBy { n -> Collections.frequency(d.value, n) })
            }

            var startSize: Int
            do {
                startSize = results.size

                depDecoders.entries().forEach { (field, pair) ->
                    if(field !in results) {
                        val otherField = pair.first
                        val decoder = pair.second
                        val otherDecoder = results[otherField] ?: return@forEach
                        val unfoldedNum: Number = when(decoder) {
                            is Int -> decoder.toInt() * otherDecoder.toInt()
                            is Long -> decoder.toLong() * otherDecoder.toLong()
                            else -> error(decoder)
                        }
                        if(unfoldedNum.isMultiplier()) {
                            results[field] = unfoldedNum
                        }
                    }
                }

                depEncoders.entries().forEach { (field, pair) ->
                    if(field !in results) {
                        val otherField = pair.first
                        val decoder = pair.second
                        val otherDecoder = results[otherField] ?: return@forEach
                        val unfoldedNum: Number = when(decoder) {
                            is Int -> decoder.toInt() * otherDecoder.toInt().invert()
                            is Long -> decoder.toLong() * otherDecoder.toLong().invert()
                            else -> error(decoder)
                        }
                        if(unfoldedNum.isMultiplier()) {
                            results[field] = unfoldedNum.invert()
                        }
                    }
                }

            } while(startSize != results.size)

            return results
        }

        private inner class ExprInterpreter : Interpreter<Expr>(ASM9) {

            private val inter = BasicInterpreter()

            override fun newValue(type: Type?): Expr? {
                val bv = inter.newValue(type) ?: return null
                return Expr(bv)
            }

            override fun copyOperation(insn: AbstractInsnNode, value: Expr): Expr {
                return value
            }

            override fun returnOperation(insn: AbstractInsnNode, value: Expr, expected: Expr) {}

            override fun newOperation(insn: AbstractInsnNode): Expr {
                val bv = inter.newOperation(insn)
                return when(insn.opcode) {
                    LDC -> {
                        insn as LdcInsnNode
                        when(insn.cst) {
                            is Int, is Long -> Expr.Const(bv, insn.cst as Number)
                            else -> Expr(bv)
                        }
                    }
                    GETSTATIC -> getField(bv, insn as FieldInsnNode)
                    else -> Expr(bv)
                }
            }

            override fun unaryOperation(insn: AbstractInsnNode, value: Expr): Expr? {
                val bv = inter.unaryOperation(insn, value.bv)
                return when(insn.opcode) {
                    GETFIELD -> getField(bv, insn as FieldInsnNode)
                    PUTSTATIC -> {
                        putField(insn as FieldInsnNode, value)
                        null
                    }
                    else -> bv?.let { Expr(it) }
                }
            }

            override fun binaryOperation(insn: AbstractInsnNode, value1: Expr, value2: Expr): Expr? {
                val bv = inter.binaryOperation(insn, value1.bv, value2.bv)
                return when(insn.opcode) {
                    IMUL, LMUL -> {
                        val ldc = value1 as? Expr.Const ?: value2 as? Expr.Const ?: return Expr(bv)
                        val field = value1 as? Expr.GetField ?: value2 as? Expr.GetField ?: return Expr.ConstMul(bv, ldc.num)
                        if(ldc.num.isMultiplier()) {
                            decoders.put("${field.insn.owner}.${field.insn.name}", ldc.num)
                        }
                        Expr.GetFieldMul(bv, field.insn, ldc.num)
                    }
                    PUTFIELD -> {
                        putField(insn as FieldInsnNode, value2)
                        null
                    }
                    else -> bv?.let { Expr(it) }
                }
            }

            override fun ternaryOperation(insn: AbstractInsnNode, value1: Expr, value2: Expr, value3: Expr): Expr? = null

            override fun naryOperation(insn: AbstractInsnNode, values: MutableList<out Expr>): Expr? {
                val bv = inter.naryOperation(insn, emptyList()) ?: return null
                return Expr(bv)
            }

            override fun merge(value1: Expr, value2: Expr): Expr {
                return if(value1.bv == value2.bv) value1 else Expr(inter.merge(value1.bv, value2.bv))
            }

            private fun getField(bv: BasicValue, insn: FieldInsnNode): Expr = when(insn.desc) {
                INT_TYPE.descriptor, LONG_TYPE.descriptor -> Expr.GetField(bv, insn)
                else -> Expr(bv)
            }

            private fun putField(insn: FieldInsnNode, value: Expr) {
                val key = "${insn.owner}.${insn.name}"
                if(value is Expr.Const) { }
                else if(value is Expr.ConstMul) {
                    if(value.num.isMultiplier()) {
                        decoders.put(key, value.num.invert())
                    }
                }
                else if(value is Expr.GetFieldMul) {
                    val srcKey = "${value.insn.owner}.${value.insn.name}"
                    decoders.remove(srcKey, value.num)
                    depDecoders.put(srcKey, key to value.num)
                    depEncoders.put(key, srcKey to value.num)
                }
            }
        }

        private open class Expr(val bv: BasicValue) : Value {

            override fun getSize() = bv.size
            override fun equals(other: Any?) = other is Expr && bv == other.bv
            override fun hashCode() = bv.hashCode()

            class Const(bv: BasicValue, val num: Number) : Expr(bv)
            class GetField(bv: BasicValue, val insn: FieldInsnNode) : Expr(bv)
            class GetFieldMul(bv: BasicValue, val insn: FieldInsnNode, val num: Number) : Expr(bv)
            class ConstMul(bv: BasicValue, val num: Number) : Expr(bv)
        }
    }

    private fun ClassGroup.removeMultipliers(decoders: Map<String, Number>) {
        for(cls in classes) {
            for(method in cls.methods) {
                method.maxStack += 2
                insertDecoderMathExprs(method, decoders)
                simplifyMultiplierMathExprs(method)
                method.maxStack -= 2
            }
        }
    }

    private fun insertDecoderMathExprs(method: MethodNode, decoders: Map<String, Number>) {
        val insns = method.instructions
        for(insn in insns.iterator()) {
            if(insn !is FieldInsnNode) continue
            if(insn.desc !in listOf(INT_TYPE.descriptor, LONG_TYPE.descriptor)) continue
            val fieldKey = "${insn.owner}.${insn.name}"
            val decoderNum = decoders[fieldKey] ?: continue
            when(insn.opcode) {
                GETFIELD, GETSTATIC -> {
                    when(insn.desc) {
                        INT_TYPE.descriptor -> {
                            when(insn.next.opcode) {
                                I2L -> insns.insertInsns(insn.next, LdcInsnNode(decoderNum.toLong().invert()), InsnNode(LMUL))
                                else -> insns.insertInsns(insn, LdcInsnNode(decoderNum.toInt().invert()), InsnNode(IMUL))
                            }
                        }
                        LONG_TYPE.descriptor -> insns.insertInsns(insn, LdcInsnNode(decoderNum.toLong().invert()), InsnNode(LMUL))
                        else -> error(insn)
                    }
                }
                PUTFIELD -> {
                    when(insn.desc) {
                        INT_TYPE.descriptor -> {
                            when(insn.previous.opcode) {
                                DUP_X1 -> {
                                    insns.insertBeforeInsns(insn.previous, LdcInsnNode(decoderNum.toInt()), InsnNode(IMUL))
                                    insns.insertInsns(insn, LdcInsnNode(decoderNum.toInt().invert()), InsnNode(IMUL))
                                }
                                else -> insns.insertBeforeInsns(insn, LdcInsnNode(decoderNum.toInt()), InsnNode(IMUL))
                            }
                        }
                        LONG_TYPE.descriptor -> {
                            when(insn.previous.opcode) {
                                DUP2_X1 -> {
                                    insns.insertBeforeInsns(insn.previous, LdcInsnNode(decoderNum.toLong()), InsnNode(LMUL))
                                    insns.insertInsns(insn, LdcInsnNode(decoderNum.toLong().invert()), InsnNode(LMUL))
                                }
                                else -> insns.insertBeforeInsns(insn, LdcInsnNode(decoderNum.toLong()), InsnNode(LMUL))
                            }
                        }
                        else -> error(insn)
                    }
                }
                PUTSTATIC -> {
                    when(insn.desc) {
                        INT_TYPE.descriptor -> {
                            when(insn.previous.opcode) {
                                DUP -> {
                                    insns.insertBeforeInsns(insn.previous, LdcInsnNode(decoderNum.toInt()), InsnNode(IMUL))
                                    insns.insertInsns(insn, LdcInsnNode(decoderNum.toInt().invert()), InsnNode(IMUL))
                                }
                                else -> insns.insertBeforeInsns(insn, LdcInsnNode(decoderNum.toInt()), InsnNode(IMUL))
                            }
                        }
                        LONG_TYPE.descriptor -> {
                            when(insn.previous.opcode) {
                                DUP2 -> {
                                    insns.insertBeforeInsns(insn.previous, LdcInsnNode(decoderNum.toLong()), InsnNode(LMUL))
                                    insns.insertInsns(insn, LdcInsnNode(decoderNum.toLong().invert()), InsnNode(LMUL))
                                }
                                else -> insns.insertBeforeInsns(insn, LdcInsnNode(decoderNum.toLong()), InsnNode(LMUL))
                            }
                        }
                        else -> error(insn)
                    }
                }
            }
        }
    }

    private fun simplifyMultiplierMathExprs(method: MethodNode) {
        val insns = method.instructions
        val interp = MathExprInterpreter()
        val analyzer = Analyzer(interp)
        analyzer.analyze(method.cls.name, method)
        for(expr in interp.exprs) {
            when(expr.insn.opcode) {
                IMUL -> insns.reduceMultiplication(expr, 1)
                LMUL -> insns.reduceMultiplication(expr, 1L)
                else -> error(expr)
            }
        }
    }

    private fun InsnList.reduceMultiplication(expr: Expr.Mul, number: Int) {
        val product = number * expr.cstExpr.number.toInt()
        val other = expr.otherExpr
        when {
            other is Expr.Mul -> {
                removeInsns(expr.insn, expr.cstExpr.insn)
                reduceMultiplication(other, product)
            }
            other is Expr.Const -> {
                removeInsns(expr.insn, expr.cstExpr.insn)
                replaceInsn(other.insn, Insn.int(product * other.number.toInt()))
            }
            other is Expr.Add -> {
                removeInsns(expr.insn, expr.cstExpr.insn)
                reduceAddition(other.a, product)
                reduceAddition(other.b, product)
            }
            product == 1 -> removeInsns(expr.insn, expr.cstExpr.insn)
            else -> replaceInsn(expr.cstExpr.insn, Insn.int(product))
        }
    }

    private fun InsnList.reduceMultiplication(expr: Expr.Mul, number: Long) {
        val product = number * expr.cstExpr.number.toLong()
        val other = expr.otherExpr
        when {
            other is Expr.Mul -> {
                removeInsns(expr.insn, expr.cstExpr.insn)
                reduceMultiplication(other, product)
            }
            other is Expr.Const -> {
                removeInsns(expr.insn, expr.cstExpr.insn)
                replaceInsn(other.insn, Insn.long(product * other.number.toLong()))
            }
            other is Expr.Add -> {
                removeInsns(expr.insn, expr.cstExpr.insn)
                reduceAddition(other.a, product)
                reduceAddition(other.b, product)
            }
            product == 1L -> removeInsns(expr.insn, expr.cstExpr.insn)
            else -> replaceInsn(expr.cstExpr.insn, Insn.long(product))
        }
    }

    private fun InsnList.reduceAddition(expr: Expr, number: Int) {
        when(expr) {
            is Expr.Const -> replaceInsn(expr.insn, Insn.int(number * expr.number.toInt()))
            is Expr.Mul -> reduceMultiplication(expr, number)
            else -> error(expr)
        }
    }

    private fun InsnList.reduceAddition(expr: Expr, number: Long) {
        when(expr) {
            is Expr.Const -> replaceInsn(expr.insn, Insn.long(number * expr.number.toLong()))
            is Expr.Mul -> reduceMultiplication(expr, number)
            else -> error(expr)
        }
    }

    private class MathExprInterpreter : Interpreter<Expr>(ASM9) {

        private val inter = SourceInterpreter()
        private val mulExprs = LinkedHashMap<AbstractInsnNode, Expr.Mul>()

        override fun newValue(type: Type?): Expr? {
            return inter.newValue(type)?.let { Expr(it) }
        }

        override fun returnOperation(insn: AbstractInsnNode, value: Expr, expected: Expr) {}

        override fun copyOperation(insn: AbstractInsnNode, value: Expr): Expr {
            return Expr(inter.copyOperation(insn, value.sv))
        }

        override fun newOperation(insn: AbstractInsnNode): Expr {
            val sv = inter.newOperation(insn)
            return when(insn.opcode) {
                LDC -> {
                    insn as LdcInsnNode
                    when(insn.cst) {
                        is Int, is Long -> Expr.Const(sv, insn.cst as Number)
                        else -> Expr(sv)
                    }
                }
                ICONST_0, LCONST_0 -> Expr.Const(sv, 0)
                ICONST_1, LCONST_1 -> Expr.Const(sv, 1)
                else -> Expr(sv)
            }
        }

        override fun unaryOperation(insn: AbstractInsnNode, value: Expr): Expr? {
            val sv = inter.unaryOperation(insn, value.sv) ?: return null
            return Expr(sv)
        }

        override fun binaryOperation(insn: AbstractInsnNode, value1: Expr, value2: Expr): Expr? {
            val sv = inter.binaryOperation(insn, value1.sv, value2.sv) ?: return null
            if(value1 == value2) return Expr(sv)
            return when(insn.opcode) {
                IMUL, LMUL -> {
                    if(value1 !is Expr.Const && value2 !is Expr.Const) {
                        Expr(sv)
                    } else {
                        Expr.Mul(sv, value1, value2).also {
                            mulExprs[insn] = it
                        }
                    }
                }
                IADD, LADD, ISUB, LSUB -> {
                    if((value1 is Expr.Const || value1 is Expr.Mul) && (value2 is Expr.Const || value2 is Expr.Mul)) {
                        Expr.Add(sv, value1, value2)
                    } else {
                        Expr(sv)
                    }
                }
                else -> Expr(sv)
            }
        }

        override fun ternaryOperation(insn: AbstractInsnNode, value1: Expr, value2: Expr, value3: Expr): Expr? {
            return null
        }

        override fun naryOperation(insn: AbstractInsnNode, values: MutableList<out Expr>): Expr? {
            val sv = inter.naryOperation(insn, emptyList()) ?: return null
            return Expr(sv)
        }

        override fun merge(value1: Expr, value2: Expr): Expr {
            if(value1 == value2) {
                return value1
            }
            else if(value1 is Expr.Mul && value2 is Expr.Mul && value1.insn == value2.insn) {
                if(value1.a == value2.a && value1.a is Expr.Const) {
                    return Expr.Mul(value1.sv, value1.a, merge(value1.b, value2.b)).also { mulExprs[value1.insn] = it }
                } else if(value1.b == value2.b && value1.b is Expr.Const) {
                    return Expr.Mul(value1.sv, merge(value1.a, value2.a), value1.b).also { mulExprs[value1.insn] = it }
                }
            }
            else if(value1 is Expr.Add && value2 is Expr.Add && value1.insn == value2.insn) {
                if(value1.a == value2.a && value1.a::class != Expr::class) {
                    val b = merge(value1.b, value2.b)
                    if(b is Expr.Const || b is Expr.Mul) {
                        return Expr.Add(value1.sv, value1.a, b)
                    }
                } else if(value1.b == value2.b && value2.b::class != Expr::class) {
                    val a = merge(value1.a, value2.a)
                    if(a is Expr.Const || a is Expr.Mul) {
                        return Expr.Add(value1.sv, a, value1.b)
                    }
                }
            }
            if(value1 is Expr.Mul) mulExprs.remove(value1.insn)
            if(value2 is Expr.Mul) mulExprs.remove(value2.insn)
            return Expr(inter.merge(value1.sv, value2.sv))
        }

        val exprs: Collection<Expr.Mul> get() {
            val ret = LinkedHashSet<Expr.Mul>()
            for(expr in mulExprs.values) {
                val other = expr.otherExpr
                if(other is Expr.Mul) {
                    ret.remove(other)
                }
                if(other is Expr.Add && other.a is Expr.Mul) {
                    ret.remove(other.a)
                }
                if(other is Expr.Add && other.b is Expr.Mul) {
                    ret.remove(other.b)
                }
                ret.add(expr)
            }
            return ret
        }
    }

    private open class Expr(val sv: SourceValue) : Value {
        override fun getSize() = sv.size
        override fun equals(other: Any?) = other is Expr && sv == other.sv
        override fun hashCode() = sv.hashCode()

        val insn get() = sv.insns.single()

        override fun toString(): String = "(#${sv.hashCode().toString(16)})"

        class Const(sv: SourceValue, val number: Number) : Expr(sv) {
            override fun toString(): String = "($number)"
        }

        class Mul(sv: SourceValue, val a: Expr, val b: Expr) : Expr(sv) {
            val cstExpr get() = a as? Const ?: b as Const
            val otherExpr get() = if(cstExpr == b) a else b
            override fun toString(): String = "($a*$b)"
        }

        class Add(sv: SourceValue, val a: Expr, val b: Expr) : Expr(sv) {
            override fun toString(): String = "($a${if(insn.opcode in listOf(IADD, LADD)) '+' else '-'}$b)"
        }
    }

    /**
     * Insn Utility Functions
     */

    private fun InsnList.insertInsns(prev: AbstractInsnNode, vararg insns: AbstractInsnNode) {
        insns.reversed().forEach { insert(prev, it) }
    }

    private fun InsnList.insertBeforeInsns(next: AbstractInsnNode, vararg insns: AbstractInsnNode) {
        insns.forEach { insertBefore(next, it) }
    }

    private fun InsnList.removeInsns(vararg insns: AbstractInsnNode) {
        insns.forEach { remove(it) }
    }

    fun InsnList.replaceInsn(old: AbstractInsnNode, new: AbstractInsnNode) {
        set(old, new)
    }

    private fun AbstractInsnNode.isIntValue(): Boolean {
        return when(opcode) {
            LDC -> (this as LdcInsnNode).cst is Int
            in listOf(SIPUSH, BIPUSH, ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5) -> true
            else -> false
        }
    }

    private val AbstractInsnNode.intValue: Int get() {
        if(opcode in 2..8) return opcode - 3
        if(opcode in listOf(BIPUSH, SIPUSH)) return (this as IntInsnNode).operand
        if(this is LdcInsnNode && cst is Int) return cst as Int
        error(this)
    }

    private object Insn {
        fun int(value: Int): AbstractInsnNode = when(value) {
            in 1..5 -> InsnNode(value + 3)
            in Byte.MIN_VALUE .. Byte.MAX_VALUE -> IntInsnNode(BIPUSH, value)
            in Short.MIN_VALUE .. Short.MAX_VALUE -> IntInsnNode(SIPUSH, value)
            else -> LdcInsnNode(value)
        }

        fun long(value: Long): AbstractInsnNode = when(value) {
            0L, 1L -> InsnNode((value + 9).toInt())
            else -> LdcInsnNode(value)
        }
    }
}

/**
 * Modulo Math Functions
 */

private val INT_MODULUS = BigInteger.ONE.shiftLeft(Int.SIZE_BITS)
private val LONG_MODULUS = BigInteger.ONE.shiftLeft(Long.SIZE_BITS)

fun Int.invert() = this.toBigInteger().modInverse(INT_MODULUS).toInt()
fun Long.invert() = this.toBigInteger().modInverse(LONG_MODULUS).toLong()
fun Number.invert(): Number = when(this) {
    is Int -> this.invert()
    is Long -> this.invert()
    else -> error(this)
}

fun Int.isInvertible() = this and 1 == 1
fun Long.isInvertible() = this.toInt().isInvertible()
fun Number.isInvertible() = when(this) {
    is Int, is Long -> this.toInt().isInvertible()
    else -> error(this)
}

fun Number.isMultiplier() = this.isInvertible() && this.invert() != this
