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

package org.spectralpowered.revtools.deobfuscator.bytecode.transformer

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import org.spectralpowered.revtools.asm.LabelMap
import org.spectralpowered.revtools.deobfuscator.bytecode.Transformer
import org.tinylog.kotlin.Logger
import java.util.*

class ControlFlowTransformer : Transformer() {

    private var blockCount = 0
    private var jumpCount = 0

    override fun transformMethod(method: MethodNode): Boolean {
        if(method.tryCatchBlocks.isEmpty()) {
            // Reorder Instructions
            val labelMap = LabelMap()
            val tcbLabelMap = IdentityHashMap<LabelNode, LabelNode>()
            val newInsns = InsnList()
            val blocks = ControlFlowGraph(method).blocks()
            method.instructions.filterIsInstance<LabelNode>().forEach { it ->
                tcbLabelMap[it] = LabelNode()
            }
            for(block in blocks) {
                blockCount++
                for(insn in block.instructions) {
                    if(insn is LabelNode) {
                        val label = tcbLabelMap[insn]
                        newInsns.add(label)
                    } else {
                        newInsns.add(insn.clone(labelMap))
                    }
                }
                if(block.next != null && block.instructions.isNotEmpty()) {
                    val lastInsn = block.instructions.last()
                    if(!lastInsn.isBlockTerminator()) {
                        val nextBlock = block.next!!
                        var firstInsn = nextBlock.instructions.first()
                        if(firstInsn !is LabelNode) {
                            firstInsn = LabelNode()
                            nextBlock.instructions.add(0, firstInsn)
                        }
                        newInsns.add(JumpInsnNode(GOTO, firstInsn as LabelNode))
                    }
                }
            }

            // Rebuild jumps
            val insns = method.instructions.toList()
            for(i in 0 until insns.size - 1) {
                val goto = insns[i]
                val jump = insns[i + 1]
                if(goto.opcode != GOTO) continue
                goto as JumpInsnNode
                if(goto.label !== jump) continue
                method.instructions.remove(goto)
                jumpCount++
            }
        }

        return false
    }

    override fun onComplete() {
        Logger.info("Reordered $blockCount method control-flow blocks.")
        Logger.info("Removed $jumpCount method control-flow jumps")
    }

    private class ControlFlowGraph(private val method: MethodNode) : Iterable<Block> {

        private val blockLabelMap = hashMapOf<LabelNode, Block>()
        private val blocks = mutableListOf<Block>()
        private lateinit var head: Block

        init {
            build()
        }

        override fun iterator(): Iterator<Block> {
            return blocks().iterator()
        }

        fun blocks(): List<Block> {
            val list = mutableListOf<Block>()
            walk(head, list, hashSetOf())
            return list.reversed()
        }

        private fun walk(cur: Block, list: MutableList<Block>, visited: HashSet<Block>) {
            val nextBlock = cur.next
            if(nextBlock != null && visited.add(nextBlock)) {
                walk(nextBlock, list, visited)
            }
            val successors = cur.successors
            successors.sort()
            for(successor in successors) {
                if(visited.add(successor)) {
                    walk(successor, list, visited)
                }
            }
            list.add(cur)
        }

        private fun build() {
            var id = 0
            head = Block()

            for(insn in method.instructions) {
                if(insn is LabelNode) {
                    blockLabelMap.computeIfAbsent(insn) {
                        val b = Block()
                        blocks.add(b)
                        return@computeIfAbsent b
                    }
                }
            }

            blocks.add(0, head)
            var cur = head
            for(insn in method.instructions) {
                if(insn is LabelNode) {
                    val next = blockLabelMap[insn]!!
                    if(next.id == -1) next.id = id++
                    if(next != cur) {
                        val lastInsn = if(cur.instructions.isEmpty()) null else cur.instructions.lastOrNull()
                        if(lastInsn == null || !lastInsn.isBlockTerminator()) {
                            next.previous = cur
                            cur.next = next
                        }
                        cur = next
                    }
                }
                cur.instructions.add(insn)
                if(insn is JumpInsnNode || insn is TableSwitchInsnNode || insn is LookupSwitchInsnNode) {
                    val jumps = when(insn) {
                        is JumpInsnNode -> listOf(insn.label)
                        is LookupSwitchInsnNode -> listOf(*insn.labels.toTypedArray(), insn.dflt)
                        is TableSwitchInsnNode -> listOf(*insn.labels.toTypedArray(), insn.dflt)
                        else -> error(insn)
                    }
                    for(labels in jumps) {
                        val next = blockLabelMap[labels]!!
                        if(next.id == -1) next.id = id++
                        next.addPredecessor(cur)
                        cur.addSuccessor(next)
                    }
                }
            }
        }
    }

    private class Block : Comparable<Block> {
        var id = -1
        val predecessors = mutableListOf<Block>()
        val successors = mutableListOf<Block>()
        var next: Block? = null
        var previous: Block? = null
        val instructions = mutableListOf<AbstractInsnNode>()

        fun addPredecessor(block: Block) {
            if(!predecessors.contains(block)) {
                predecessors.add(block)
            }
        }

        fun addSuccessor(block: Block) {
            if(!successors.contains(block)) {
                successors.add(block)
            }
        }

        override fun compareTo(other: Block): Int {
            val line1 = this.lineNumber
            val line2 = other.lineNumber
            if(line1 == line2 || line1 == -1 || line2 == -1) {
                return 0
            }
            return line1.compareTo(line2)
        }

        private val lineNumber: Int get() {
            for(insn in instructions) {
                if(insn is LineNumberNode) {
                    return insn.line
                }
            }
            return -1
        }
    }

    companion object {
        private fun AbstractInsnNode.isBlockTerminator() = when(this) {
            is TableSwitchInsnNode -> true
            is LookupSwitchInsnNode -> true
            is JumpInsnNode -> opcode == GOTO
            else -> opcode in listOf(ATHROW, IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, RETURN, RET)
        }
    }
}