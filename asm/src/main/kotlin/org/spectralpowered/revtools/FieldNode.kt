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

package org.spectralpowered.revtools

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.spectralpowered.revtools.util.field
import org.spectralpowered.revtools.util.isPrivate
import org.spectralpowered.revtools.util.isStatic

var FieldNode.cls: ClassNode by field()
val FieldNode.pool get() = cls.pool

val FieldNode.key get() = "${cls.key}.$name"

fun FieldNode.isPrivate() = access.isPrivate()
fun FieldNode.isStatic() = access.isStatic()