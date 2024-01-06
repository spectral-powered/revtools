package org.spectralpowered.revtools.asm.expr

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.spectralpowered.revtools.asm.expr.impl.BasicExpr
import org.spectralpowered.revtools.asm.group
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


class ExprTree(val method: MethodNode) : Iterable<BasicExpr> {

    val group get() = method.group

    val expressions = mutableListOf<BasicExpr>()

    constructor(method: MethodNode, expressions: Collection<BasicExpr>) : this(method) {
        this.expressions.addAll(expressions)
    }

    val size get() = expressions.size

    val instructions: InsnList get() {
        val insns = InsnList()
        for(expr in expressions) {
            for(insn in expr.instructions) {
                insns.add(insn)
            }
        }
        return insns
    }

    override fun iterator(): Iterator<BasicExpr> = expressions.iterator()

    fun addExpr(expr: BasicExpr) {
        expressions.add(expr)
    }

    fun addExpr(expr: BasicExpr, index: Int) {
        expressions.add(index, expr)
    }

    fun addExprs(exprs: Collection<BasicExpr>) {
        for(expr in exprs) addExpr(expr)
    }

    fun removeExpr(expr: BasicExpr) {
        val prev = expr.prev
        val next = expr.next
        expressions.remove(expr)
        next?.prev = prev
        prev?.next = next
    }

    fun removeExprs(exprs: Collection<BasicExpr>) {
        for(expr in exprs) removeExpr(expr)
    }

    companion object {

        fun build(method: MethodNode): ExprTree {
            val tree = ExprTree(method)

            val stack = mutableListOf<AbstractInsnNode>()
            stack.addAll(method.instructions.toArray())
            stack.reverse()

            val exprList = mutableListOf<BasicExpr>()
            val stackIdx = AtomicInteger(0)
            for(insn in stack) {
                val expr = BasicExpr.fromInsn(insn)
                expr.tree = tree
                expr.index = stackIdx.getAndIncrement()
                expr.size = resolveSize(insn)
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
                    prev.prev = expr
                }
                exprs.addFirst(expr)
                prev = expr
            }

            tree.addExprs(exprs.toList())
            return tree
        }

        private fun handleExpr(exprList: MutableList<BasicExpr>, parentIdx: Int, idx: AtomicInteger): Int {
            val expr = if(idx.get() >= exprList.size) null else exprList[idx.get()]
            var consume = 0
            if(expr != null) {
                if(parentIdx != -1) {
                    exprList[parentIdx].addChild(expr)
                }
                if(expr.opcode == GOTO) {
                    if(expr.parent != null) {
                        consume = expr.parent!!.size
                    }
                } else {
                    if(expr.opcode in listOf(POP2, DUP_X1)) {
                        consume = 1
                    } else if(expr.opcode in listOf(DUP2, DUP_X2, DUP2_X1, DUP2_X2)) {
                        consume = if(isDoubleOrLong(exprList[idx.get() + 1].insn)) 1 else 2
                    }
                    var prev: BasicExpr? = null
                    var i = 0
                    while(i < expr.size) {
                        idx.incrementAndGet()
                        val child = if(exprList.size > idx.get()) exprList[idx.get()] else null
                        if(child != null && prev != null) {
                            child.next = prev
                            prev.prev = child
                        }
                        i += handleExpr(exprList, expr.index, idx)
                        prev = child
                        i++
                    }
                }
            } else {
                throw IllegalStateException("Expr @ ${idx.get()} : ${exprList.first().tree.method.name}")
            }
            return consume
        }

        private fun isDoubleOrLong(insn: AbstractInsnNode): Boolean {
            val op = insn.opcode
            if (op == LCONST_0 || op == LCONST_1 || op == DCONST_0 || op == DCONST_1 || op == I2L || op == F2L || op == D2L || op == L2D || op == F2D || op == I2D || op == LADD || op == LSUB || op == LMUL || op == LDIV || op == DADD || op == DSUB || op == DMUL || op == DDIV || op == LOR || op == LAND || op == LREM || op == LNEG || op == LSHL || op == LSHR || op == LLOAD || op == DLOAD || op == LSTORE || op == DSTORE) {
                return true
            } else if (op == GETFIELD || op == GETSTATIC) {
                val fin: FieldInsnNode = insn as FieldInsnNode
                if (fin.desc.equals("J") || fin.desc.equals("D")) {
                    return true
                }
            } else if (op == INVOKESTATIC || op == INVOKEVIRTUAL || op == INVOKEDYNAMIC) {
                val min: MethodInsnNode = insn as MethodInsnNode
                if (min.desc.endsWith(")J") || min.desc.endsWith(")D")) {
                    return true
                }
            } else if (op == LDC) {
                val ldc: LdcInsnNode = insn as LdcInsnNode
                if (ldc.cst != null && (ldc.cst is Long || ldc.cst is Double)) {
                    return true
                }
            }
            return false
        }

        private fun resolveSize(insn: AbstractInsnNode): Int {
            return when (insn.opcode) {
                NOP -> {
                    0
                }
                ACONST_NULL, ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5, LCONST_0, LCONST_1, FCONST_0, FCONST_1, FCONST_2, DCONST_0, DCONST_1, BIPUSH, SIPUSH, LDC -> {
                    0
                }
                ILOAD, LLOAD, FLOAD, DLOAD, ALOAD -> {
                    0
                }
                IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD -> {
                    2
                }
                ISTORE, LSTORE, FSTORE, DSTORE, ASTORE -> {
                    1
                }
                IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE -> {
                    3
                }
                POP -> {
                    1
                }
                POP2 -> {
                    if (isDoubleOrLong(insn.previous)) {
                        1
                    } else 2
                }
                DUP -> {
                    1
                }
                DUP_X1 -> {
                    2
                }
                DUP_X2 -> {
                    dup_x2(insn)
                }
                DUP2 -> {
                    dup2(insn)
                }
                DUP2_X1 -> {
                    dup2_x1(insn)
                }
                DUP2_X2 -> {
                    dup2_x2(insn)
                }
                SWAP -> {
                    2
                }
                IADD, LADD, FADD, DADD, ISUB, LSUB, FSUB, DSUB, IMUL, LMUL, FMUL, DMUL, IDIV, LDIV, FDIV, DDIV, IREM, LREM, FREM, DREM -> {
                    2
                }
                INEG, LNEG, FNEG, DNEG -> {
                    1
                }
                ISHL, LSHL, ISHR, LSHR, IUSHR, LUSHR, IAND, LAND, IOR, LOR, IXOR, LXOR -> {
                    2
                }
                IINC -> {
                    1
                }
                I2L, I2F, I2D, L2I, L2F, L2D, F2I, F2L, F2D, D2I, D2L, D2F, I2B, I2C, I2S -> {
                    1
                }
                LCMP, FCMPL, FCMPG, DCMPL, DCMPG -> {
                    2
                }
                IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE -> {
                    1
                }
                IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE -> {
                    2
                }
                GOTO -> {
                    0
                }
                JSR -> {
                    0
                }
                RET -> {
                    0
                }
                TABLESWITCH, LOOKUPSWITCH -> {
                    1
                }
                IRETURN, LRETURN, FRETURN, DRETURN, ARETURN -> {
                    1
                }
                RETURN -> {
                    0
                }
                GETSTATIC -> {
                    0
                }
                PUTSTATIC -> {
                    1
                }
                GETFIELD -> {
                    1
                }
                PUTFIELD -> {
                    2
                }
                INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, INVOKEINTERFACE, INVOKEDYNAMIC -> {
                    val desc: String = (insn as MethodInsnNode).desc
                    var count = 0
                    var i: Int = Type.getArgumentTypes(desc).size
                    while (i > 0) {
                        count++
                        --i
                    }
                    if (insn.opcode != INVOKESTATIC) {
                        count++
                    }
                    count
                }
                NEW -> {
                    0
                }
                NEWARRAY, ANEWARRAY, ARRAYLENGTH -> {
                    1
                }
                ATHROW -> {
                    1
                }
                CHECKCAST, INSTANCEOF -> {
                    1
                }
                MONITORENTER, MONITOREXIT -> {
                    1
                }
                MULTIANEWARRAY -> {
                    (insn as MultiANewArrayInsnNode).dims
                }
                IFNULL, IFNONNULL -> {
                    1
                }
                else -> when (insn) {
                    is LabelNode -> return 0
                    is LineNumberNode -> return 0
                    is FrameNode -> return 0
                    else -> throw RuntimeException("Illegal opcode " + insn.opcode + " - " + insn)
                }
            }
        }

        private fun dup2(insn: AbstractInsnNode): Int {
            var size2: Boolean = isDoubleOrLong(insn.previous)
            if (!size2) {
                size2 = isDoubleOrLong(insn.previous.previous)
                if (!size2) {
                    return 2
                }
            } else {
                return 1
            }
            throw IllegalStateException("Illegal use of DUP2 @ $insn")
        }

        private fun dup_x2(insn: AbstractInsnNode): Int {
            var size2: Boolean = isDoubleOrLong(insn.previous)
            if (!size2) {
                size2 = isDoubleOrLong(insn.previous.previous)
                if (!size2) {
                    size2 = isDoubleOrLong(insn.previous.previous.previous)
                    if (!size2) {
                        return 3
                    }
                } else {
                    return 2
                }
            }
            throw IllegalStateException("Illegal use of DUP_X2 @ $insn")
        }

        private fun dup2_x1(insn: AbstractInsnNode): Int {
            var size2: Boolean = isDoubleOrLong(insn.previous)
            if (!size2) {
                size2 = isDoubleOrLong(insn.previous.previous)
                if (!size2) {
                    size2 = isDoubleOrLong(insn.previous.previous.previous)
                    if (!size2) {
                        return 3
                    }
                }
            } else {
                size2 = isDoubleOrLong(insn.previous.previous)
                return if (!size2) {
                    2
                } else {
                    3
                }
            }
            throw IllegalStateException("Illegal use of DUP2_X1 @ $insn")
        }

        private fun dup2_x2(insn: AbstractInsnNode): Int {
            var size2: Boolean = isDoubleOrLong(insn.previous)
            if (!size2) {
                size2 = isDoubleOrLong(insn.previous.previous)
                if (!size2) {
                    size2 = isDoubleOrLong(insn.previous.previous.previous)
                    if (!size2) {
                        size2 = isDoubleOrLong(insn.previous.previous.previous.previous)
                        if (!size2) {
                            return 4
                        }
                    } else {
                        return 3
                    }
                }
            } else {
                size2 = isDoubleOrLong(insn.previous.previous)
                if (!size2) {
                    size2 = isDoubleOrLong(insn.previous.previous.previous)
                    if (!size2) {
                        return 3
                    }
                } else {
                    return 2
                }
            }
            throw IllegalStateException("Illegal use of DUP2_X2 @ $insn")
        }
    }
}