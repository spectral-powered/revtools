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

package org.spectralpowered.revtools.asm.ir.value

import org.spectralpowered.revtools.asm.ir.BasicBlock

interface User

interface ValueUser : User {
    fun replaceUsesOf(ctx: ValueUsageContext, from: UsableValue, to: UsableValue)
    fun clearValueUses(ctx: ValueUsageContext)
}

interface BlockUser : User {
    fun replaceUsesOf(ctx: BlockUsageContext, from: UsableBlock, to: UsableBlock)
    fun clearBlockUses(ctx: BlockUsageContext)
}

abstract class Usable<T> {
    abstract fun get(): T
}

abstract class UsableValue : Usable<Value>()
abstract class UsableBlock : Usable<BasicBlock>()
