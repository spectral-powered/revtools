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

import java.util.AbstractCollection

fun <T> stackOf(vararg elements: T) = Stack(elements.toList())

@Suppress("unused")
class Stack<T>(elements: Collection<T>) : AbstractCollection<T>() {
    private val inner = dequeOf(elements)

    override val size: Int
        get() = inner.size

    constructor() : this(listOf())

    fun push(element: T) = inner.push(element)
    fun pop(): T = inner.pop()
    fun peek(): T = inner.peek()

    fun popOrNull(): T? = if (isEmpty()) null else pop()

    override fun contains(element: T) = element in inner
    override fun containsAll(elements: Collection<T>) = inner.containsAll(elements)
    override fun isEmpty() = inner.isEmpty()
    override fun iterator() = inner.iterator()
}
