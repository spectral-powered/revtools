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

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.spectralpowered.revtools.*

class ClassNodeTest : StringSpec({

    "isPublic should return true for public class" {
        val classNode = ClassNode().apply {
            access = Opcodes.ACC_PUBLIC
        }

        classNode.isPublic().shouldBe(true)
    }

    "isPublic should return false for non-public class" {
        val classNode = ClassNode().apply {
            access = Opcodes.ACC_PRIVATE
        }

        classNode.isPublic().shouldBe(false)
    }

    "isPrivate should return true for private class" {
        val classNode = ClassNode().apply {
            access = Opcodes.ACC_PRIVATE
        }

        classNode.isPrivate().shouldBe(true)
    }

    "isPrivate should return false for non-private class" {
        val classNode = ClassNode().apply {
            access = Opcodes.ACC_PUBLIC
        }

        classNode.isPrivate().shouldBe(false)
    }

    "isAbstract should return true for abstract class" {
        val classNode = ClassNode().apply {
            access = Opcodes.ACC_ABSTRACT
        }

        classNode.isAbstract().shouldBe(true)
    }

    "isAbstract should return false for non-abstract class" {
        val classNode = ClassNode().apply {
            access = Opcodes.ACC_PUBLIC
        }

        classNode.isAbstract().shouldBe(false)
    }

    "isInterface should return true for interface class" {
        val classNode = ClassNode().apply {
            access = Opcodes.ACC_INTERFACE
        }

        classNode.isInterface().shouldBe(true)
    }

    "isInterface should return false for non-interface class" {
        val classNode = ClassNode().apply {
            access = Opcodes.ACC_PUBLIC
        }

        classNode.isInterface().shouldBe(false)
    }

    "getMethod should return method with matching name and descriptor" {
        val classNode = ClassNode().apply {
            methods.add(MethodNode(Opcodes.ASM5, "testMethod", "()V", null, null))
        }

        classNode.getMethod("testMethod", "()V").shouldBe(classNode.methods[0])
    }

    "getMethod should return null for non-existing method" {
        val classNode = ClassNode()

        classNode.getMethod("testMethod", "()V").shouldBe(null)
    }

    "getField should return field with matching name and descriptor" {
        val classNode = ClassNode().apply {
            fields.add(FieldNode(Opcodes.ASM5, "testField", "I", null, null))
        }

        classNode.getField("testField", "I").shouldBe(classNode.fields[0])
    }

    "getField should return null for non-existing field" {
        val classNode = ClassNode()

        classNode.getField("testField", "I").shouldBe(null)
    }

})

