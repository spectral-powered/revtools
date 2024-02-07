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

@file:Suppress("MemberVisibilityCanBePrivate")

package org.spectralpowered.revtools.asm.helper.collection

class Subset<T : Any>(val data: T?) {
    var parent = this
        internal set
    var rank = 0
        internal set

    fun isRoot() = parent == this

    fun getRoot(): Subset<T> = if (!isRoot()) {
        val ancestor = parent.getRoot()
        parent = ancestor
        ancestor
    } else this

    override fun hashCode() = System.identityHashCode(this)
    override fun equals(other: Any?) = this === other
    override fun toString() = "Subset $data"
}

@Suppress("unused")
class DisjointSet<T : Any>(private val children: MutableSet<Subset<T>> = mutableSetOf()) : MutableSet<Subset<T>> by children {
    fun find(element: Subset<T>) = element.getRoot()
    fun findUnsafe(element: Subset<T>?) = element?.getRoot()

    private fun Subset<T>.merge(other: Subset<T>): Subset<T> = when {
        this == other -> this
        this.rank < other.rank -> {
            this.parent = other
            other
        }
        this.rank > other.rank -> {
            other.parent = this
            this
        }
        else -> {
            other.parent = this
            ++this.rank
            this
        }
    }

    fun join(lhv: Subset<T>, rhv: Subset<T>): Subset<T> {
        val lhvRoot = find(lhv)
        val rhvRoot = find(rhv)

        return lhvRoot.merge(rhvRoot)
    }

    fun joinUnsafe(lhv: Subset<T>?, rhv: Subset<T>?): Subset<T>? {
        val lhvRoot = findUnsafe(lhv)
        val rhvRoot = findUnsafe(rhv)

        if (lhvRoot == null) return null
        if (rhvRoot == null) return null

        return lhvRoot.merge(rhvRoot)
    }

    fun emplace(element: T?): Subset<T> {
        val wrapped = Subset(element)
        add(wrapped)
        return wrapped
    }
}
