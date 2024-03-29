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

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

data class MemberRef(val owner: String, val name: String, val desc: String) {
    constructor(cls: ClassNode, name: String, desc: String) : this(cls.name, name, desc)
    constructor(cls: ClassNode, method: MethodNode) : this(cls.name, method.name, method.desc)
    constructor(cls: ClassNode, field: FieldNode) : this(cls.name, field.name, field.desc)
    constructor(owner: String, memberDesc: MemberDesc) : this(owner, memberDesc.name, memberDesc.desc)
    constructor(methodInsn: MethodInsnNode) : this(methodInsn.owner, methodInsn.name, methodInsn.desc)
    constructor(fieldInsn: FieldInsnNode) : this(fieldInsn.owner, fieldInsn.name, fieldInsn.desc)

    override fun toString(): String {
        return "$owner $name $desc"
    }
}