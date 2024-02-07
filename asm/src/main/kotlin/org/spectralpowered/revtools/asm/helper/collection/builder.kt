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

@file:Suppress("DEPRECATION", "unused")

package org.spectralpowered.revtools.asm.helper.collection

@Deprecated("Use built-in kotlin builders instead")
interface MutableBuilder<T> : MutableCollection<T> {
    val inner: MutableCollection<T>

    operator fun T.unaryPlus() {
        inner += this
    }

    operator fun Collection<T>.unaryPlus() {
        inner += this
    }
}

@Deprecated("Use built-in kotlin builders instead")
open class ListBuilder<T>(
    override val inner: MutableList<T> = mutableListOf()
) : MutableBuilder<T>, MutableList<T> by inner

@Deprecated("Use built-in kotlin builders instead")
open class SetBuilder<T>(
    override val inner: MutableSet<T> = mutableSetOf()
) : MutableBuilder<T>, MutableSet<T> by inner

@Deprecated("Use built-in kotlin builders instead")
fun <T> buildList(init: ListBuilder<T>.() -> Unit): List<T> {
    val builder = ListBuilder<T>()
    builder.init()
    return builder.inner
}

fun <T> listOf(action: () -> T): List<T> = listOf(action())
fun <T> listOf(size: Int, action: (Int) -> T): List<T> = (0 until size).map(action)
fun <T> listOf(vararg actions: () -> T): List<T> = actions.map { it() }

@Deprecated("Use built-in kotlin builders instead")
fun <T> buildSet(init: SetBuilder<T>.() -> Unit): Set<T> {
    val builder = SetBuilder<T>()
    builder.init()
    return builder.inner
}

fun <T> setOf(action: () -> T): Set<T> = setOf(action())
fun <T> setOf(size: Int, action: (Int) -> T): Set<T> = (0 until size).mapTo(mutableSetOf(), action)
fun <T> setOf(vararg actions: () -> T): Set<T> = actions.mapTo(mutableSetOf()) { it() }


fun <T> buildMutableList(builder: MutableList<T>.() -> Unit): MutableList<T> {
    val res = mutableListOf<T>()
    res.builder()
    return res
}

fun <T> buildMutableSet(builder: MutableSet<T>.() -> Unit): MutableSet<T> {
    val res = mutableSetOf<T>()
    res.builder()
    return res
}

fun <K, V> buildMutableMap(builder: MutableMap<K, V>.() -> Unit): MutableMap<K, V> {
    val res = mutableMapOf<K, V>()
    res.builder()
    return res
}
