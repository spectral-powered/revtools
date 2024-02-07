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

import info.leadinglight.jdot.Edge
import info.leadinglight.jdot.Graph
import info.leadinglight.jdot.Node
import info.leadinglight.jdot.enums.Color
import info.leadinglight.jdot.enums.Shape
import info.leadinglight.jdot.impl.Util
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

interface Viewable {
    val graphView: List<GraphView>

    @Suppress("unused")
    fun view(name: String, dot: String, viewer: String): String =
        Util.sh(arrayOf(viewer).plus("file://${toFile(name, dot)}"))

    fun Viewable.toFile(name: String, dot: String): Path =
        toFile(name, dot) {
            setBgColor(Color.X11.transparent)
            setFontSize(12.0)
            setFontName("Fira Mono")
        }

    fun Viewable.toFile(name: String, dot: String, graphConfigurator: Graph.() -> Unit): Path {
        Graph.setDefaultCmd(dot)

        val graph = Graph(name)

        graph.addNodes(*graphView.map { vertex ->
            val node = Node(vertex.name).setShape(Shape.box).setLabel(vertex.label).setFontSize(12.0)
            vertex.nodeConfigurator(node)
            node
        }.toTypedArray())

        graph.graphConfigurator()

        for (node in graphView) {
            for ((successor, label, configurator) in node.successors) {
                graph.addEdge(Edge(node.name, successor.name).also {
                    it.setLabel(label)
                    configurator(it)
                })
            }
        }
        val file = graph.dot2file("svg")
        val newFile = "${file.removeSuffix("out")}svg"
        val resultingFile = File(newFile).toPath()
        Files.move(File(file).toPath(), resultingFile)
        return resultingFile
    }
}

data class GraphView(
    val name: String,
    val label: String,
    val nodeConfigurator: (Node) -> Unit = {}
) {
    val successors: List<Triple<GraphView, String, (Edge) -> Unit>> get() = mutableSuccessors
    private val mutableSuccessors = mutableListOf<Triple<GraphView, String, (Edge) -> Unit>>()

    fun addSuccessor(successor: GraphView, label: String = "", edgeConfigurator: (Edge) -> Unit = {}) {
        mutableSuccessors += Triple(successor, label, edgeConfigurator)
    }
}
