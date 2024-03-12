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

package org.spectralpowered.revtools.asm.remap

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.spectralpowered.revtools.asm.InsnMatcher
import org.spectralpowered.revtools.asm.util.toAsmName

private val INVOKE_MATCHER = InsnMatcher.compile("LDC INVOKESTATIC")

private fun matchClassNodeName(match: List<AbstractInsnNode>): Boolean {
    val ldc = match[0] as LdcInsnNode
    if(ldc.cst !is String) return false

    val invokestatic = match[1] as MethodInsnNode
    return invokestatic.owner == "java/lang/Class" &&
            invokestatic.name == "forName" &&
            invokestatic.desc == "(Ljava/lang/String;)Ljava/lang/String;"
}

private fun findLdcInsns(method: MethodNode): Sequence<LdcInsnNode> {
    return INVOKE_MATCHER.match(method)
        .filter(::matchClassNodeName)
        .map { it[0] as LdcInsnNode }
}

private fun internalName(ldc: LdcInsnNode): String {
    return (ldc.cst as String).toAsmName()
}

fun findClassNames(method: MethodNode): Sequence<String> {
    return findLdcInsns(method).map(::internalName)
}

fun remap(remapper: AsmRemapper, method: MethodNode) {
    for(ldc in findLdcInsns(method)) {
        val name = remapper.mapType(internalName(ldc))
    }
}