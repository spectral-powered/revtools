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

import org.spectralpowered.revtools.asm.helper.KtException
import org.spectralpowered.revtools.asm.helper.collection.queueOf
import org.spectralpowered.revtools.asm.helper.collection.stackOf
import java.util.ArrayDeque

class NoTopologicalSortingException(msg: String) : KtException(msg)

@Suppress("MemberVisibilityCanBePrivate")
class GraphTraversal<T : Graph.Vertex<T>>(private val graph: Graph<T>) {
    private enum class Colour { WHITE, GREY, BLACK }

    fun <R> dfs(start: T, action: (T) -> R): List<R> {
        val search = mutableListOf<R>()
        val colours = mutableMapOf<T, Colour>()
        val stack = stackOf<T>()
        stack.push(start)
        while (stack.isNotEmpty()) {
            val top = stack.pop()
            if (colours.getOrPut(top) { Colour.WHITE } == Colour.WHITE) {
                colours[top] = Colour.BLACK
                search.add(action(top))
                top.successors.filter { colours[it] != Colour.BLACK }.forEach { stack.push(it) }
            }
        }
        return search
    }

    fun dfs(start: T = graph.entry): List<T> = dfs(start) { it }

    fun <R> bfs(start: T, action: (T) -> R): List<R> {
        val search = mutableListOf<R>()
        val colours = mutableMapOf<T, Colour>()
        val queue = queueOf<T>()
        queue.add(start)
        while (queue.isNotEmpty()) {
            val top = queue.poll()
            if (colours.getOrPut(top) { Colour.WHITE } == Colour.WHITE) {
                colours[top] = Colour.BLACK
                search.add(action(top))
                top.successors.filter { colours[it] != Colour.BLACK }.forEach { queue.add(it) }
            }
        }
        return search
    }

    fun bfs(start: T = graph.entry): List<T> = bfs(start) { it }

    fun <R> topologicalSort(start: T, action: (T) -> R): List<R> {
        val order = arrayListOf<R>()
        val colors = hashMapOf<T, Colour>()

        fun dfs(node: T) {
            val stack = ArrayDeque<Pair<T, Boolean>>()
            stack.push(node to false)
            while (stack.isNotEmpty()) {
                val (top, isPostprocessing) = stack.poll()
                if (colors[top] == Colour.BLACK)
                    continue

                if (isPostprocessing) {
                    colors[top] = Colour.BLACK
                    order += action(top)
                } else {
                    stack.push(top to true)
                    colors[top] = Colour.GREY
                    for (edge in top.successors) {
                        val currentColour = colors.getOrPut(edge) { Colour.WHITE }
                        if (currentColour == Colour.GREY)
                            throw NoTopologicalSortingException("Could not perform topological sort")
                        else if (currentColour != Colour.BLACK)
                            stack.push(edge to false)
                    }
                }
            }
        }

        dfs(start)
        return order.reversed()
    }

    fun topologicalSort(start: T = graph.entry): List<T> = topologicalSort(start) { it }
}
