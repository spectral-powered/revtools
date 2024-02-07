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

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.LookupSwitchInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TableSwitchInsnNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue
import org.spectralpowered.revtools.deobfuscator.Logger
import org.spectralpowered.revtools.deobfuscator.Transformer
import org.spectralpowered.revtools.deobfuscator.asm.tree.ClassGroup
import org.spectralpowered.revtools.deobfuscator.asm.tree.cls
import java.util.Stack

class ControlFlowOptimizer : Transformer {

    private var count = 0

    override fun run(group: ClassGroup) {
        for(cls in group.classes) {
            for(method in cls.methods) {
                if(method.tryCatchBlocks.isNotEmpty()) continue
                val blocks = ControlFlowGraph(method).blocks
                val insns = InsnList()
                if(blocks.isNotEmpty()) {
                    val labelMap = LabelMap()
                    val queue = Stack<Block>()
                    val placed = hashSetOf<Block>()
                    queue.add(blocks.first())
                    while(queue.isNotEmpty()) {
                        val block = queue.pop()
                        if(block in placed) continue
                        placed.add(block)
                        block.branches.sortedBy { it.lineNumber }.forEach { queue.push(it.root) }
                        if(block.next != null) queue.push(block.next)
                        for(i in block.startIndex until block.endIndex) {
                            insns.add(method.instructions[i].clone(labelMap))
                        }
                    }
                }
                count += blocks.size
                method.instructions = insns
            }
        }

        Logger.info("Reordered $count method control-flow blocks.")
    }

    private class ControlFlowGraph(method: MethodNode) : Analyzer<BasicValue>(BasicInterpreter()) {

        val blocks = mutableListOf<Block>()

        init {
            analyze(method.cls.name, method)
        }

        override fun init(owner: String, method: MethodNode) {
            val insns = method.instructions
            var block = Block()
            block.id = blocks.size + 1
            blocks.add(block)
            for(i in 0 until insns.size()) {
                val insn = insns[i]
                block.instructions.add(insn)
                block.endIndex++
                if(insn.next == null) break
                if(insn.next is LabelNode || insn is JumpInsnNode || insn is TableSwitchInsnNode || insn is LookupSwitchInsnNode) {
                    block = Block()
                    block.id = blocks.size + 1
                    block.startIndex = i + 1
                    block.endIndex = i + 1
                    blocks.add(block)
                }
            }
        }

        override fun newControlFlowEdge(from: Int, to: Int) {
            val fromBlock = blocks.first { from in it.startIndex until it.endIndex }
            val toBlock = blocks.first { to in it.startIndex until it.endIndex }
            if(fromBlock != toBlock) {
                if(from + 1 == to) {
                    fromBlock.next = toBlock
                    toBlock.prev = fromBlock
                } else {
                    fromBlock.branches.add(toBlock)
                }
            }
        }
    }

    private class Block {

        var id = -1

        var startIndex = 0
        var endIndex = 0

        var prev: Block? = null
        var next: Block? = null
        val branches = mutableListOf<Block>()

        val instructions = mutableListOf<AbstractInsnNode>()

        val lineNumber: Int get() {
            for(insn in instructions) {
                if(insn is LineNumberNode) {
                    return insn.line
                }
            }
            return -1
        }

        val root: Block get() {
            var cur = this
            var last = prev
            while(last != null) {
                cur = last
                last = cur.prev
            }
            return cur
        }
    }

    private class LabelMap : AbstractMap<LabelNode, LabelNode>() {
        private val map = hashMapOf<LabelNode, LabelNode>()
        override val entries get() = throw IllegalStateException()
        override fun get(key: LabelNode) = map.getOrPut(key) { LabelNode() }
    }
}