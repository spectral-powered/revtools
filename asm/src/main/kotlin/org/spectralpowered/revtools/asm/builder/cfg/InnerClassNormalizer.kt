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

package org.spectralpowered.revtools.asm.builder.cfg

import org.spectralpowered.revtools.asm.ClassGroup
import org.spectralpowered.revtools.asm.ir.Class
import org.spectralpowered.revtools.asm.visitor.ClassVisitor

class InnerClassNormalizer(override val group: ClassGroup) : ClassVisitor {
    override fun cleanup() {}

    override fun visit(klass: Class) {
        if (klass.cn.innerClasses == null) return
        val iterator = klass.cn.innerClasses.iterator()
        while (iterator.hasNext()) {
            val innerClassNode = iterator.next()
            if (innerClassNode.outerName != klass.cn.name && innerClassNode.outerName != null) {
                iterator.remove()
                val outer = group[innerClassNode.outerName]
                val inner = group[innerClassNode.name]
                if (outer.cn.innerClasses.all { it.name != innerClassNode.name }) outer.cn.innerClasses.add(innerClassNode)
                if (inner.cn.outerClass != innerClassNode.outerName) inner.cn.outerClass = innerClassNode.outerName
            }
        }
    }
}
