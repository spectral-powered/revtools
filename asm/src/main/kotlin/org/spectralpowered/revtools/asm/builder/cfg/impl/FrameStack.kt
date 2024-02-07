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

package org.spectralpowered.revtools.asm.builder.cfg.impl

import org.spectralpowered.revtools.asm.ir.value.UsableValue
import org.spectralpowered.revtools.asm.ir.value.UsageContext
import org.spectralpowered.revtools.asm.ir.value.Value
import org.spectralpowered.revtools.asm.ir.value.ValueUsageContext
import org.spectralpowered.revtools.asm.ir.value.ValueUser

internal class FrameStack(
    private val ctx: UsageContext,
    private val stack: MutableList<Value> = mutableListOf()
) : ValueUser, MutableList<Value> by stack, UsageContext by ctx {
    override fun replaceUsesOf(ctx: ValueUsageContext, from: UsableValue, to: UsableValue) {
        stack.replaceAll { if (it == from) to.get() else it }
    }

    override fun add(element: Value): Boolean {
        element.addUser(this)
        return stack.add(element)
    }

    override fun add(index: Int, element: Value) {
        element.addUser(this)
        return stack.add(index, element)
    }

    override fun addAll(index: Int, elements: Collection<Value>): Boolean {
        elements.forEach { it.addUser(this) }
        return stack.addAll(index, elements)
    }

    override fun addAll(elements: Collection<Value>): Boolean {
        elements.forEach { it.addUser(this) }
        return stack.addAll(elements)
    }

    override fun clear() {
        stack.forEach { it.removeUser(this) }
        stack.clear()
    }

    override fun remove(element: Value): Boolean {
        stack.forEach {
            if (it == element) it.removeUser(this)
        }
        return stack.remove(element)
    }

    override fun removeAll(elements: Collection<Value>): Boolean {
        val removeSet = elements.toSet()
        stack.forEach {
            if (it in removeSet) it.removeUser(this)
        }
        return stack.removeAll(removeSet)
    }

    override fun removeAt(index: Int): Value {
        val res = stack.removeAt(index)
        res.removeUser(this)
        return res
    }

    override fun retainAll(elements: Collection<Value>): Boolean {
        val removeSet = elements.toSet()
        stack.forEach {
            if (it !in removeSet) it.removeUser(this)
        }
        return stack.retainAll(removeSet)
    }

    override fun set(index: Int, element: Value): Value {
        element.addUser(this)
        val res = stack.set(index, element)
        res.removeUser(this)
        return res
    }

    override fun clearValueUses(ctx: ValueUsageContext) {
        stack.forEach { it.removeUser(this) }
    }
}
