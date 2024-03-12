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

package org.spectralpowered.revtools.asm.analysis

import org.jgrapht.Graph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.EdgeReversedGraph
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.MethodNode
import org.spectralpowered.revtools.asm.util.UniqueQueue

abstract class DataFlowAnalyzer<T>(val method: MethodNode, backwards: Boolean = false) {
    private val graph: Graph<Int, DefaultEdge>
    private val inSets = mutableMapOf<Int, T>()
    private val outSets = mutableMapOf<Int, T>()

    init {
        val forwardsGraph = ControlFlowAnalyzer.create(method)
        graph = when(backwards) {
            true -> EdgeReversedGraph(forwardsGraph)
            else -> forwardsGraph
        }
    }

    abstract fun createInitialSet(): T
    open fun createEntrySet(): T = createInitialSet()

    abstract fun join(set1: T, set2: T): T
    abstract fun transfer(set: T, insn: AbstractInsnNode): T

    fun getInSet(index: Int) = inSets[index]
    fun getInSet(insn: AbstractInsnNode) = getInSet(method.instructions.indexOf(insn))

    fun getOutSet(index: Int) = outSets[index]
    fun getOutSet(insn: AbstractInsnNode) = getOutSet(method.instructions.indexOf(insn))

    fun analyze() {
        val entrySet = createEntrySet()
        val initialSet = createInitialSet()

        val queue = UniqueQueue<Int>()
        queue.addAll(graph.vertexSet().filter { vert -> graph.inDegreeOf(vert) == 0 })

        while(true) {
            val node = queue.removeFirstOrNull() ?: break

            val predecessors = graph.incomingEdgesOf(node).map { edge ->
                outSets[graph.getEdgeSource(edge)] ?: initialSet
            }

            val inSet = if(predecessors.isEmpty()) {
                entrySet
            } else {
                predecessors.reduce(this::join)
            }

            inSets[node] = inSet

            val outSet = transfer(inSet, method.instructions[node])
            if(outSets[node] != outSet) {
                outSets[node] = outSet

                for(edge in graph.outgoingEdgesOf(node)) {
                    val successor = graph.getEdgeTarget(edge)
                    queue.add(successor)
                }
            }
        }
    }
}