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

package org.spectralpowered.revtools.asm.util

class UniqueQueue<T> {
    private val queue = ArrayDeque<T>()
    private val set = mutableSetOf<T>()

    fun add(value: T): Boolean {
        if(set.add(value)) {
            queue.addLast(value)
            return true
        }
        return false
    }

    fun addAll(values: Iterable<T>) {
        for(value in values) {
            add(value)
        }
    }

    operator fun plusAssign(value: T) {
        add(value)
    }

    operator fun plusAssign(values: Iterable<T>) {
        addAll(values)
    }

    fun removeFirstOrNull(): T? {
        val value = queue.removeFirstOrNull()
        if(value != null) {
            set.remove(value)
            return value
        }
        return null
    }

    fun clear() {
        queue.clear()
        set.clear()
    }
}