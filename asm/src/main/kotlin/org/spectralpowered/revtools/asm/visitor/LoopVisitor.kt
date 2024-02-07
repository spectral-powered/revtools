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

package org.spectralpowered.revtools.asm.visitor

import org.spectralpowered.revtools.asm.ClassGroup
import org.spectralpowered.revtools.asm.helper.assert.asserted
import org.spectralpowered.revtools.asm.helper.graph.GraphView
import org.spectralpowered.revtools.asm.helper.graph.LoopDetector
import org.spectralpowered.revtools.asm.helper.graph.PredecessorGraph
import org.spectralpowered.revtools.asm.helper.graph.Viewable
import org.spectralpowered.revtools.asm.ir.BasicBlock
import org.spectralpowered.revtools.asm.ir.CatchBlock
import org.spectralpowered.revtools.asm.ir.Method

interface LoopVisitor : MethodVisitor {
    val preservesLoopInfo get() = false

    override fun visit(method: Method): Unit = try {
        val loops = group.loopManager.getMethodLoopInfo(method)
        loops.forEach { visitLoop(it) }
    } finally {
        updateLoopInfo(method)
    }

    fun visitLoop(loop: Loop) {
        for (it in loop.subLoops) visitLoop(it)
    }

    fun updateLoopInfo(method: Method) {
        if (!this.preservesLoopInfo) {
            group.loopManager.setInvalid(method)
        }
    }
}

data class LoopNode(
    val parent: Loop,
    val block: BasicBlock
) : PredecessorGraph.PredecessorVertex<LoopNode> {
    override val predecessors: Set<LoopNode>
        get() = block.predecessors
            .filter { it in parent.body }
            .mapTo(mutableSetOf()) { LoopNode(parent, it) }

    override val successors: Set<LoopNode>
        get() = block.successors
            .filter { it in parent.body }
            .mapTo(mutableSetOf()) { LoopNode(parent, it) }
}

@Suppress("MemberVisibilityCanBePrivate", "unused")
class Loop(
    val header: BasicBlock,
    val body: MutableSet<BasicBlock>
) : PredecessorGraph<LoopNode>, Iterable<LoopNode>, Viewable {
    internal var parentUnsafe: Loop? = null

    val parent get() = asserted(hasParent) { parentUnsafe!! }
    val hasParent get() = parentUnsafe != null

    val subLoops = hashSetOf<Loop>()

    override val entry: LoopNode
        get() = LoopNode(this, header)

    override val nodes: Set<LoopNode>
        get() = body.mapTo(mutableSetOf()) { LoopNode(this, it) }

    val method: Method?
        get() = header.methodUnsafe

    val allEntries: Set<BasicBlock>
        get() = body.filterNotTo(mutableSetOf()) {
            when (it) {
                is CatchBlock -> body.containsAll(it.allPredecessors)
                else -> body.containsAll(it.predecessors)
            }
        }

    val exitingBlocks: Set<BasicBlock>
        get() = body.filterNotTo(mutableSetOf()) { body.containsAll(it.successors) }

    val loopExits: Set<BasicBlock>
        get() = body.flatMap { it.successors }.filterNotTo(mutableSetOf()) { body.contains(it) }

    val preheaders: List<BasicBlock>
        get() = header.predecessors.filter { !body.contains(it) }

    val preheader: BasicBlock
        get() = preheaders.first()

    val latches: Set<BasicBlock>
        get() = body.filterTo(mutableSetOf()) { it.successors.contains(header) }

    val latch: BasicBlock
        get() = latches.first()

    val hasSinglePreheader get() = preheaders.size == 1
    val hasSingleLatch get() = body.filterTo(mutableSetOf()) { it.successors.contains(header) }.size == 1

    fun containsAll(blocks: Collection<LoopNode>) = body.containsAll(blocks.map { it.block })

    fun addBlock(bb: BasicBlock) {
        body.add(bb)
        parentUnsafe?.addBlock(bb)
    }

    fun addSubLoop(loop: Loop) = subLoops.add(loop)
    fun removeBlock(bb: BasicBlock) {
        body.remove(bb)
        parentUnsafe?.removeBlock(bb)
    }

    override fun iterator() = nodes.iterator()

    operator fun contains(block: BasicBlock) = block in body

    override val graphView: List<GraphView>
        get() {
            val views = hashMapOf<String, GraphView>()

            nodes.forEach { node ->
                views[node.block.name.toString()] = GraphView(node.block.name.toString(), "${node.block.name}\\l")
            }

            nodes.forEach {
                val current = views.getValue(it.block.name.toString())
                for (successor in it.successors) {
                    current.addSuccessor(views.getValue(successor.block.name.toString()))
                }
            }

            return views.values.toList()
        }
}


fun performLoopAnalysis(method: Method): List<Loop> {
    val la = LoopAnalysis(method.group)
    return la.invoke(method)
}

class LoopAnalysis(override val group: ClassGroup) : MethodVisitor {
    private val loops = arrayListOf<Loop>()

    override fun cleanup() {
        loops.clear()
    }

    operator fun invoke(method: Method): List<Loop> {
        visit(method)
        return loops
    }

    override fun visit(method: Method) {
        cleanup()

        val allLoops = LoopDetector(method.body).search().map { Loop(it.key, it.value.toMutableSet()) }

        val parents = hashMapOf<Loop, MutableSet<Loop>>()
        for (loop in allLoops) {
            for (parent in allLoops) {
                val set = parents.getOrPut(loop, ::hashSetOf)
                if (loop != parent && loop.header in parent)
                    set.add(parent)
            }
        }
        loops.addAll(parents.filter { it.value.isEmpty() }.keys)

        var numLoops = loops.size
        while (numLoops < allLoops.size) {
            val remove = hashSetOf<Loop>()
            val removableParents = hashSetOf<Loop>()

            for ((child, possibleParents) in parents) {
                if (possibleParents.size == 1) {
                    possibleParents.first().addSubLoop(child)
                    child.parentUnsafe = possibleParents.first()

                    remove.add(child)
                    removableParents.add(possibleParents.first())
                    ++numLoops
                }
            }
            remove.forEach { parents.remove(it) }
            for (it in removableParents) {
                for ((_, possibleParents) in parents) {
                    possibleParents.remove(it)
                }
            }
        }

        for (loop in allLoops) {
            val headers = loop.body.count { !it.predecessors.all { pred -> pred in loop } }
            require(headers == 1) { "Only loops with single header are supported" }
        }
    }
}
