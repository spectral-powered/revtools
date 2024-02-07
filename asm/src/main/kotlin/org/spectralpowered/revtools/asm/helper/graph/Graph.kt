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

@file:Suppress("unused")

package org.spectralpowered.revtools.asm.helper.graph

interface Graph<T : Graph.Vertex<T>> {
    val entry: T
    val nodes: Set<T>

    interface Vertex<out T : Vertex<T>> {
        val successors: Set<T>
    }

    fun findEntries(): Set<T> {
        val hasEntry = nodes.associateWith { false }.toMutableMap()
        for (node in nodes) {
            node.successors.forEach { hasEntry[it] = true }
        }
        return hasEntry.filter { !it.value }.keys
    }
}

interface PredecessorGraph<T : PredecessorGraph.PredecessorVertex<T>> : Graph<T> {
    interface PredecessorVertex<out T : PredecessorVertex<T>> : Graph.Vertex<T> {
        val predecessors: Set<T>
    }
}

fun <T : Graph.Vertex<T>> Set<T>.asGraph(): Graph<T> = object : Graph<T> {
    override val entry: T
        get() = this@asGraph.first()
    override val nodes = this@asGraph
}
