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

package org.spectralpowered.revtools.deobfuscator.bytecode.transformer

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode
import org.spectralpowered.revtools.deobfuscator.bytecode.Transformer
import org.tinylog.kotlin.Logger

class FinalClassTransformer : Transformer() {

    private val superClasses = mutableListOf<String>()
    private var count = 0

    override fun transformClass(cls: ClassNode): Boolean {
        val superClass = cls.superName
        if(superClass != null) {
            superClasses += superClass
        }
        superClasses.addAll(cls.interfaces)
        return false
    }

    override fun postTransform(): Boolean {
        for(cls in pool.classes) {
            val access = cls.access
            if(cls.checkFinal()) {
                cls.access = access or ACC_FINAL
            } else {
                cls.access = access and ACC_FINAL.inv()
            }
            if(cls.access != access) {
                count++
            }
        }
        return false
    }

    override fun onComplete() {
        Logger.info("Changed $count classes final modifiers.")
    }

    private fun ClassNode.checkFinal(): Boolean {
        if((access and (ACC_ABSTRACT or ACC_INTERFACE)) != 0) {
            return false
        }
        return !superClasses.contains(name)
    }
}