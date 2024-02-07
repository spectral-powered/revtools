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

@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package org.spectralpowered.revtools.asm.helper.collection

class MapWithDefault<K, V>(
    private val inner: Map<K, V>,
    val default: V
) : Map<K, V> by inner {
    override fun get(key: K): V = inner[key] ?: default
}

class MutableMapWithDefault<K, V>(
    private val inner: MutableMap<K, V>,
    val default: V
) : MutableMap<K, V> by inner {
    override fun get(key: K): V = inner[key] ?: default
}


fun <K, V> Map<K, V>.withDefault(default: V) = MapWithDefault(this, default)
fun <K, V> Map<K, V>.withDefault(default: () -> V) = MapWithDefault(this, default())

fun <K, V> MutableMap<K, V>.withDefault(default: V) = MutableMapWithDefault(this, default)
fun <K, V> MutableMap<K, V>.withDefault(default: () -> V) = MutableMapWithDefault(this, default())
