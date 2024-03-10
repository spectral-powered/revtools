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

package org.spectralpowered.revtools.deobfuscator.bytecode

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.spectralpowered.revtools.asm.ClassPool

abstract class Transformer {

    val name = javaClass.simpleName.removeSuffix("Transformer")
    lateinit var pool: ClassPool private set

    open fun transform(pool: ClassPool) {
        this.pool = pool

        onStart()
        var changed: Boolean
        do {
           changed = preTransform()
           for(cls in pool.classes) {
               changed = changed or transformClass(cls)
               for(method in cls.methods) {
                   changed = changed or transformMethod(method)
               }
               for(field in cls.fields) {
                   changed = changed or transformField(field)
               }
           }
            changed = changed or postTransform()
        } while(changed)
        onComplete()
    }

    open fun onStart() {}

    open fun preTransform(): Boolean {
        return false
    }

    open fun postTransform(): Boolean {
        return false
    }

    open fun transformClass(cls: ClassNode): Boolean {
        return false
    }

    open fun transformMethod(method: MethodNode): Boolean {
        return false
    }

    open fun transformField(field: FieldNode): Boolean {
        return false
    }

    open fun onComplete() {}
}