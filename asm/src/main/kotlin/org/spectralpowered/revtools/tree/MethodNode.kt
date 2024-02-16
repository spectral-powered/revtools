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

package org.spectralpowered.revtools.tree

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.spectralpowered.revtools.util.*
import org.spectralpowered.revtools.util.isAbstract
import org.spectralpowered.revtools.util.isInterface
import org.spectralpowered.revtools.util.isPrivate

var MethodNode.cls: ClassNode by field()
val MethodNode.pool get() = cls.pool

val MethodNode.key get() = "${cls.key}.$name$desc"

fun MethodNode.isPrivate() = access.isPrivate()
fun MethodNode.isAbstract() = access.isAbstract()
fun MethodNode.isInterface() = access.isInterface()
fun MethodNode.isStatic() = access.isStatic()