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

public interface DisjointSet<T> : Iterable<DisjointSet.Partition<T>> {
    public interface Partition<T> : Iterable<T>

    public val elements: Int
    public val partitions: Int

    public fun add(x: T): Partition<T>
    public operator fun get(x: T): Partition<T>?
    public fun union(x: Partition<T>, y: Partition<T>)
}

public class ForestDisjointSet<T> : DisjointSet<T> {
    private class Node<T>(val value: T) : DisjointSet.Partition<T> {
        val children = mutableListOf<Node<T>>()
        private var _parent = this
        var parent
            get() = _parent
            set(parent) {
                _parent = parent
                _parent.children.add(this)
            }
        var rank = 0

        fun find(): Node<T> {
            if (parent !== this) {
                _parent = parent.find()
            }
            return parent
        }

        override fun iterator(): Iterator<T> {
            return NodeIterator(find())
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Node<*>) return false

            return find() === other.find()
        }

        override fun hashCode(): Int {
            return find().value.hashCode()
        }

        override fun toString(): String {
            return find().value.toString()
        }
    }

    private class NodeIterator<T>(root: Node<T>) : Iterator<T> {
        private val queue = ArrayDeque<Node<T>>()

        init {
            queue.add(root)
        }

        override fun hasNext(): Boolean {
            return queue.isNotEmpty()
        }

        override fun next(): T {
            val node = queue.removeFirstOrNull() ?: throw NoSuchElementException()
            queue.addAll(node.children)
            return node.value
        }
    }

    private val nodes = mutableMapOf<T, Node<T>>()
    override val elements: Int
        get() = nodes.size
    override var partitions: Int = 0
        private set

    override fun add(x: T): DisjointSet.Partition<T> {
        val node = findNode(x)
        if (node != null) {
            return node
        }

        partitions++

        val newNode = Node(x)
        nodes[x] = newNode
        return newNode
    }

    override fun get(x: T): DisjointSet.Partition<T>? {
        return findNode(x)
    }

    private fun findNode(x: T): Node<T>? {
        val node = nodes[x] ?: return null
        return node.find()
    }

    override fun union(x: DisjointSet.Partition<T>, y: DisjointSet.Partition<T>) {
        require(x is Node<T>)
        require(y is Node<T>)

        val xRoot = x.find()
        val yRoot = y.find()

        if (xRoot == yRoot) {
            return
        }

        when {
            xRoot.rank < yRoot.rank -> {
                xRoot.parent = yRoot
            }

            xRoot.rank > yRoot.rank -> {
                yRoot.parent = xRoot
            }

            else -> {
                yRoot.parent = xRoot
                xRoot.rank++
            }
        }

        partitions--
    }

    override fun iterator(): Iterator<DisjointSet.Partition<T>> {
        return nodes.values.iterator()
    }
}