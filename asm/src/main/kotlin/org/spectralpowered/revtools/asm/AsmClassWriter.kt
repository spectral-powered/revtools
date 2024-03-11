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

package org.spectralpowered.revtools.asm

import org.objectweb.asm.ClassWriter

class AsmClassWriter(private val pool: ClassPool) : ClassWriter(COMPUTE_MAXS) {
    override fun getCommonSuperClass(cls1: String, cls2: String): String {
        try {
            return super.getCommonSuperClass(cls1, cls2)
        } catch (e: Exception) {
            if(pool.containsClass(cls1) && pool.containsClass(cls2)) {
                val super1 = pool.findClass(cls1)!!.superName
                val super2 = pool.findClass(cls2)!!.superName
                if(super1 == super2) return super1
            }
            return "java/lang/Object"
        }
    }
}