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

package org.spectralpowered.revtools.asm.helper.collection

import org.spectralpowered.revtools.asm.helper.assert.ktassert

interface Cache<K, V> {
    val maxSize: UInt
    val size: UInt
    operator fun set(key: K, value: V)
    operator fun get(key: K): V?
    operator fun contains(key: K): Boolean
    fun clear()
}


@Suppress("unused")
private class DoublyLinkedList<T> {
    var head: Node? = null
    var tail: Node? = null

    inner class Node(var item: T) {
        var previous: Node? = null
        var next: Node? = null
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            @Suppress("UNCHECKED_CAST")
            other as DoublyLinkedList<T>.Node
            return item == other.item
        }

        override fun hashCode(): Int {
            return item?.hashCode() ?: 0
        }
    }

    fun add(element: T): Node {
        val node = Node(element)
        if (head == null) {
            head = node
        } else if (tail == null) {
            tail = node
            head!!.next = tail
            tail!!.previous = head
        } else {
            tail!!.next = node
            node.previous = tail
            tail = node
        }
        return node
    }

    fun moveToFront(node: Node) {
        if (node == head) return
        val temp = head!!
        head!!.item = node.item
        head!!.previous = node.previous
        head!!.next = node.next

        node.item = temp.item
        node.previous = temp.previous
        node.next = temp.next
    }

    fun clear() {
        var current = head
        while (current != null) {
            val next = current.next
            current.previous = null
            current.next = null
            current = next
        }
        head = null
        tail = null
    }

    fun removeFirst(): Node? {
        return when {
            head == null -> null
            tail == null -> head.also {
                head = null
            }

            else -> tail.also {
                val next = head!!.next
                next!!.previous = null
                tail!!.previous = null
                head!!.next = null
                head = next
            }
        }
    }

    fun removeLast(): Node? {
        return when {
            head == null -> null
            tail == null -> head.also {
                head = null
            }

            tail?.previous == head -> tail.also {
                head!!.next = null
                tail!!.previous = null
                tail = null
            }

            else -> tail.also {
                val previous = tail!!.previous
                previous!!.next = null
                tail!!.previous = null
                tail!!.next = null
                tail = previous
            }
        }
    }
}

class LRUCache<K, V>(override val maxSize: UInt) : Cache<K, V> {
    val cache = HashMap<K, V>()
    private val query = DoublyLinkedList<K>()
    private val nodeMap = HashMap<K, DoublyLinkedList<K>.Node>()

    override val size: UInt get() = cache.size.toUInt()

    init {
        ktassert(maxSize > 0U, "Cache should have positive size")
    }

    override fun set(key: K, value: V) {
        val shouldRemove = key !in cache
        while (shouldRemove && cache.size >= maxSize.toInt()) {
            val last = query.removeLast() ?: break
            nodeMap.remove(last.item)
            cache.remove(last.item)
        }

        val node = nodeMap.getOrPut(key) { query.add(key) }
        cache[key] = value
        query.moveToFront(node)
    }

    override fun get(key: K): V? {
        return cache[key]
    }

    override fun contains(key: K): Boolean {
        return key in cache
    }


    override fun clear() {
        cache.clear()
        query.clear()
        nodeMap.clear()
    }
}
