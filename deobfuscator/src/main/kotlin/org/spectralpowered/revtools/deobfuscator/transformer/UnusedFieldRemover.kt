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

package org.spectralpowered.revtools.deobfuscator.transformer

import org.objectweb.asm.Opcodes.ACC_FINAL
import org.objectweb.asm.tree.FieldInsnNode
import org.spectralpowered.revtools.deobfuscator.Logger
import org.spectralpowered.revtools.deobfuscator.Transformer
import org.spectralpowered.revtools.deobfuscator.asm.tree.ClassGroup
import org.spectralpowered.revtools.deobfuscator.asm.tree.key

class UnusedFieldRemover : Transformer {

    private var count = 0

    override fun run(group: ClassGroup) {
        val usedFields = group.classes.flatMap { it.methods }
            .flatMap { it.instructions.toArray().asIterable() }
            .mapNotNull { it as? FieldInsnNode }
            .map { "${it.owner}.${it.name}" }
            .toSet()

        for(cls in group.classes) {
            val itr = cls.fields.iterator()
            while(itr.hasNext()) {
                val field = itr.next()
                if(field.key !in usedFields && (field.access and ACC_FINAL) != 0) {
                    count++
                    itr.remove()
                }
            }
        }

        Logger.info("Removed $count unused fields.")
    }
}