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

import org.spectralpowered.revtools.asm.ir.value.*

internal class LocalArray(
    private val ctx: UsageContext,
    private val locals: MutableMap<Int, Value> = hashMapOf()
) : ValueUser, MutableMap<Int, Value> by locals, UsageContext by ctx {
    private val valueMapping = hashMapOf<Value, MutableSet<Int>>()
    override fun clear() {
        values.forEach { it.removeUser(this) }
        locals.clear()
    }

    override fun put(key: Int, value: Value): Value? {
        value.addUser(this)
        val prev = locals.put(key, value)
        prev?.let {
            it.removeUser(this)
            valueMapping.getOrPut(it, ::mutableSetOf).remove(key)
        }
        valueMapping.getOrPut(value, ::mutableSetOf).add(key)
        return prev
    }

    override fun putAll(from: Map<out Int, Value>) {
        for ((key, value) in from) {
            put(key, value)
        }
    }

    override fun remove(key: Int): Value? {
        val res = locals.remove(key)
        res?.let {
            it.removeUser(this)
            valueMapping.getOrPut(it, ::mutableSetOf).remove(key)
        }
        return res
    }

    override fun replaceUsesOf(ctx: ValueUsageContext, from: UsableValue, to: UsableValue) {
        val fromKeys = valueMapping.getOrPut(from.get(), ::mutableSetOf)
        val toKeys = valueMapping.getOrPut(to.get(), ::mutableSetOf)
        for (key in fromKeys) {
            from.get().removeUser(this)
            locals[key] = to.get()
            to.addUser(this)
            toKeys.add(key)
        }
        fromKeys.clear()
    }

    override fun clearValueUses(ctx: ValueUsageContext) {
        entries.forEach { it.value.removeUser(this) }
    }
}
