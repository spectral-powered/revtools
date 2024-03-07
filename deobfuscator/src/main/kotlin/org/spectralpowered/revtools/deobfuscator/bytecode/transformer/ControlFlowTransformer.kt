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

import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import org.spectralpowered.revtools.ClassPool
import org.spectralpowered.revtools.deobfuscator.bytecode.BytecodeTransformer
import org.spectralpowered.revtools.isTerminal
import org.spectralpowered.revtools.util.LabelMap
import org.tinylog.kotlin.Logger

class ControlFlowTransformer : BytecodeTransformer {

    private var blockCount = 0
    private var jumpCount = 0

    override fun run(pool: ClassPool) {
        for(cls in pool.classes) {
            for(method in cls.methods) {
                if(method.tryCatchBlocks.isNotEmpty()) continue
                method.normalizeInsns()
                method.normalizeJumps()
            }
        }
    }

    override fun postRun() {
        Logger.info("Normalized $blockCount method control-flow blocks.")
        Logger.info("Removed $jumpCount redundant method GOTO jumps.")
    }

    private fun MethodNode.normalizeInsns() {
        val cfg = ControlFlowGraph(this)
        val newInsns = InsnList()
        val blocks = cfg.sortedBlocks
        val labelMap = LabelMap()

        for(block in blocks) {
            blockCount++
            for(insn in block.instructions) {
                newInsns.add(insn.clone(labelMap))
            }
            if(block.next != null && block.instructions.isNotEmpty()) {
                val insn = block.instructions[block.instructions.size - 1]
                if(!insn.isTerminal()) {
                    val next = block.next!!
                    var nextEntryInsn = next.instructions.first()
                    if(nextEntryInsn !is LabelNode) {
                        nextEntryInsn = LabelNode(Label())
                        next.instructions.add(0, nextEntryInsn)
                    }
                    newInsns.add(JumpInsnNode(GOTO, nextEntryInsn.clone(labelMap) as LabelNode))
                }
            }
        }
        instructions = newInsns
    }

    private fun MethodNode.normalizeJumps() {
        val insns = instructions.toList()
        for(i in 0 until insns.size - 1) {
            val insn = insns[i]
            val gotoInsn = insns[i + 1]
            if(insn.opcode != GOTO) continue
            insn as JumpInsnNode
            if(insn.label !== gotoInsn) continue
            instructions.remove(insn)
            jumpCount++
        }
    }

    private class ControlFlowGraph(private val method: MethodNode) {

        private val blockMap = hashMapOf<LabelNode, Block>()
        val blocks = mutableListOf<Block>()
        lateinit var head: Block private set

        init {
            build()
        }

        val sortedBlocks: List<Block> get() {
            val list = mutableListOf<Block>()
            walk(head, list, hashSetOf())
            return list.reversed()
        }

        private fun walk(cur: Block, order: MutableList<Block>, visited: HashSet<Block>) {
            val next = cur.next
            if(next != null && visited.add(next)) {
                walk(cur.next!!, order, visited)
            }
            val successors = cur.successors
            successors.sortWith { a, b -> a.compare(b) }
            for(successor in successors) {
                if(visited.add(successor)) {
                    walk(successor, order, visited)
                }
            }
            order.add(cur)
        }

        private fun build() {
            var id = 0
            head = Block()

            for(insn in method.instructions) {
                if(insn is LabelNode) {
                    blockMap.computeIfAbsent(insn) {
                        Block().also { blocks.add(it) }
                    }
                }
            }

            blocks.add(0, head)
            var cur = head
            for(insn in method.instructions) {
                if(insn is LabelNode) {
                    val next = blockMap[insn]!!
                    if(next.id == -1) {
                        next.id = id++
                    }
                    if(next != cur) {
                        val last = if(cur.instructions.isEmpty()) null else cur.instructions[cur.instructions.size - 1]
                        if(last == null || !last.isTerminal()) {
                            next.previous = cur
                            cur.next = next
                        }
                        cur = next
                    }
                }
                cur.instructions.add(insn)
                if(insn is JumpInsnNode || insn is LookupSwitchInsnNode || insn is TableSwitchInsnNode) {
                    val jumps = mutableListOf<LabelNode>()
                    when(insn) {
                        is JumpInsnNode -> jumps.add(insn.label)
                        is LookupSwitchInsnNode -> jumps.addAll(insn.labels.plus(insn.dflt))
                        is TableSwitchInsnNode -> jumps.addAll(insn.labels.plus(insn.dflt))
                    }
                    for(label in jumps) {
                        val next = blockMap[label]!!
                        if(next.id == -1) {
                            next.id = id++
                        }
                        cur.addSuccessor(next)
                        next.addPredecessor(cur)
                    }
                }
            }
        }
    }

    private class Block {

        var id = -1
        val predecessors = mutableListOf<Block>()
        val successors = mutableListOf<Block>()
        var previous: Block? = null
        var next: Block? = null
        val instructions = mutableListOf<AbstractInsnNode>()

        fun addPredecessor(block: Block) {
            if(!predecessors.contains(block)) predecessors.add(block)
        }

        fun addSuccessor(block: Block) {
            if(!successors.contains(block)) successors.add(block)
        }

        fun compare(other: Block): Int {
            val line1 = lineNumber
            val line2 = other.lineNumber
            return if(line1 == line2 || line1 == -1 || line2 == -1) {
                0
            } else {
                line1.compareTo(line2)
            }
        }

        private val lineNumber: Int get() {
            for(insn in instructions) {
                if(insn is LabelNode) {
                    if((insn.next as? LineNumberNode) != null) {
                        return (insn.next as LineNumberNode).line
                    }
                }
            }
            return -1
        }
    }
}