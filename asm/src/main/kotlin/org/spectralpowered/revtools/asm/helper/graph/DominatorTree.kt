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

package org.spectralpowered.revtools.asm.helper.graph

import org.spectralpowered.revtools.asm.helper.collection.queueOf
import org.spectralpowered.revtools.asm.helper.collection.stackOf
import org.spectralpowered.revtools.asm.helper.tree.Tree
import kotlin.math.min

@Suppress("unused")
class DominatorTreeNode<T : Graph.Vertex<T>>(val value: T) : Tree.TreeNode<DominatorTreeNode<T>> {
    var idom: DominatorTreeNode<T>? = null
        internal set
    private val dominates = hashSetOf<DominatorTreeNode<T>>()
    private val dominationCache = hashSetOf<T>()

    override val children: Set<DominatorTreeNode<T>>
        get() = dominates
    override val parent get() = idom

    fun dominates(node: T): Boolean {
        val queue = queueOf(this)
        while (queue.isNotEmpty()) {
            val current = queue.poll()
            if (node in current.dominationCache) return true
            queue.addAll(current.dominates)
        }
        return false
    }

    internal fun addDomineer(node: DominatorTreeNode<T>) {
        dominates += node
        dominationCache += node.value
    }

    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DominatorTreeNode<*>) return false

        return value == other.value
    }
}

class DominatorTree<T : Graph.Vertex<T>>
    : MutableMap<T, DominatorTreeNode<T>> by mutableMapOf(), Viewable {
    override val graphView: List<GraphView>
        get() {
            val nodes = hashMapOf<String, GraphView>()
            this.keys.forEach {
                nodes[it.toString().replace("\"", "\\\"")] =
                    GraphView(
                        it.toString().replace("\"", "\\\""),
                        it.toString().replace("\"", "\\\"")
                    )
            }

            this.entries.forEach { (node, tnode) ->
                val current = nodes.getValue(node.toString().replace("\"", "\\\""))
                val idom = tnode.idom?.value?.toString()
                if (idom != null) {
                    val parent = nodes.getValue(idom.replace("\"", "\\\""))
                    parent.addSuccessor(current)
                }
            }

            return nodes.values.toList()
        }
}

@Suppress("unused")
class DominatorTreeBuilder<T : Graph.Vertex<T>>(private val graph: Graph<T>) {
    private val tree = DominatorTree<T>()

    private var nodeCounter: Int = 0
    private val dfsTree = hashMapOf<T, Int>()
    private val reverseMapping = arrayListOf<T?>()
    private val reverseGraph = arrayListOf<ArrayList<Int>>()
    private val parents = arrayListOf<Int>()
    private val labels = arrayListOf<Int>()
    private val sDom = arrayListOf<Int>()
    private val dom = arrayListOf<Int>()
    private val dsu = arrayListOf<Int>()
    private val bucket = arrayListOf<MutableSet<Int>>()

    init {
        for (i in graph.nodes) {
            parents.add(-1)
            labels.add(-1)
            sDom.add(-1)
            dom.add(-1)
            dsu.add(-1)
            reverseMapping.add(null)
            dfsTree[i] = -1
            bucket.add(mutableSetOf())
            reverseGraph.add(arrayListOf())
        }
        tree.putAll(graph.nodes.map { it to DominatorTreeNode(it) })
    }

    private fun union(u: Int, v: Int) {
        dsu[v] = u
    }

    private fun find(u: Int, x: Int = 0): Int {
        val stack = stackOf<Pair<Int, Int>>()
        var currentV: Int
        // search til we reach the bottom u
        run {
            var currentU = u
            var currentX = x
            while (true) {
                if (currentU < 0) {
                    currentV = currentU
                    break
                }
                if (currentU == dsu[currentU]) {
                    currentV = if (currentX != 0) -1 else currentU
                    break
                }
                stack.push(currentU to currentX)
                currentU = dsu[currentU]
                currentX++
            }
        }

        // return back and recompute everything for the U's
        while (stack.isNotEmpty()) {
            val (currentU, currentX) = stack.pop()
            if (currentV < 0) {
                currentV = currentU
                continue
            }
            if (sDom[labels[dsu[currentU]]] < sDom[labels[currentU]]) labels[currentU] = labels[dsu[currentU]]
            dsu[currentU] = currentV
            currentV = if (currentX != 0) currentV else labels[currentU]
        }
        return currentV
    }

    // correct recursive implementation (which fail with stack overflow on big graphs)
    private fun findRecursive(u: Int, x: Int = 0): Int {
        if (u < 0) return u
        if (u == dsu[u]) return if (x != 0) -1 else u
        val v = findRecursive(dsu[u], x + 1)
        if (v < 0) return u
        if (sDom[labels[dsu[u]]] < sDom[labels[u]]) labels[u] = labels[dsu[u]]
        dsu[u] = v
        return if (x != 0) v else labels[u]
    }

    private fun dfsRecursive(node: T) {
        dfsTree[node] = nodeCounter
        reverseMapping[nodeCounter] = node
        labels[nodeCounter] = nodeCounter
        sDom[nodeCounter] = nodeCounter
        dsu[nodeCounter] = nodeCounter
        nodeCounter++
        for (it in node.successors) {
            if (dfsTree.getValue(it) == -1) {
                dfsRecursive(it)
                parents[dfsTree.getValue(it)] = dfsTree.getValue(node)
            }
            reverseGraph[dfsTree.getValue(it)].add(dfsTree.getValue(node))
        }
    }

    private fun dfs(node: T) {
        val stack = stackOf<Pair<T, Int>>()
        stack.push(node to -1)

        while (stack.isNotEmpty()) {
            val (top, parent) = stack.pop()
            if (dfsTree.getValue(top) != -1) {
                if (parent >= 0) reverseGraph[dfsTree.getValue(top)].add(parent)
                continue
            }

            dfsTree[top] = nodeCounter
            reverseMapping[nodeCounter] = top
            labels[nodeCounter] = nodeCounter
            sDom[nodeCounter] = nodeCounter
            dsu[nodeCounter] = nodeCounter
            nodeCounter++
            if (parent >= 0) {
                parents[dfsTree.getValue(top)] = parent
                reverseGraph[dfsTree.getValue(top)].add(parent)
            }

            for (it in top.successors.reversed()) {
                if (dfsTree.getValue(it) == -1) {
                    stack.push(it to dfsTree.getValue(top))
                } else {
                    reverseGraph[dfsTree.getValue(it)].add(dfsTree.getValue(top))
                }
            }
        }
    }

    fun build(): DominatorTree<T> {
        for (it in graph.nodes) if (dfsTree[it] == -1) dfs(it)
        val n = dfsTree.size
        for (i in n - 1 downTo 0) {
            for (j in reverseGraph[i]) {
                sDom[i] = min(sDom[i], sDom[find(j)])
            }
            if (i > 0) bucket[sDom[i]].add(i)
            for (j in bucket[i]) {
                val v = find(j)
                if (sDom[v] == sDom[j]) dom[j] = sDom[j]
                else dom[j] = v
            }
            if (i > 0) union(parents[i], i)
        }
        for (i in 1 until n) {
            if (dom[i] != sDom[i]) dom[i] = dom[dom[i]]
        }
        for ((it, idom) in dom.withIndex()) {
            val current = reverseMapping[it]!!
            if (idom != -1) {
                val dominator = reverseMapping[idom]!!
                tree.getValue(dominator).addDomineer(tree.getValue(current))
                tree.getValue(current).idom = tree.getValue(dominator)
            }
        }
        for (it in tree) {
            if (it.key == it.value.idom?.value) it.value.idom = null
        }
        return tree
    }
}
