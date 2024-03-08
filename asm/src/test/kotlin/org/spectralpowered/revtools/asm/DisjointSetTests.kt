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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.spectralpowered.revtools.util.DisjointSet

class DisjointSetTest : FunSpec({
    context("DisjointSet") {
        lateinit var disjointSet: DisjointSet<Int>

        beforeEach {
            disjointSet = DisjointSet()
        }

        test("add should add new elements to the set") {
            disjointSet.add(1)
            disjointSet.add(2)
            disjointSet.add(3)

            disjointSet[1]?.value shouldBe 1
            disjointSet[2]?.value shouldBe 2
            disjointSet[3]?.value shouldBe 3
        }

        test("add should not add duplicate elements to the set") {
            disjointSet.add(1)
            disjointSet.add(1)

            disjointSet[1]?.value shouldBe 1
        }

        test("get should return the node of the element if it exists in the set") {
            disjointSet.add(1)

            disjointSet[1]?.value shouldBe 1
        }

        test("get should return null if the element does not exist in the set") {
            disjointSet[4] shouldBe null
        }

        test("join should join two subsets into a single subset") {
            val node1 = disjointSet.add(1)
            val node2 = disjointSet.add(2)

            disjointSet.join(node1, node2)

            node1.find() shouldBe node2.find()
        }

        test("join should not join two subsets if they are already in the same subset") {
            val node1 = disjointSet.add(1)
            val node2 = disjointSet.add(2)

            disjointSet.join(node1, node2)
            disjointSet.join(node1, node2)

            node1.find() shouldBe node2.find()
        }

        test("join should increase rank if both subsets have the same rank") {
            val node1 = disjointSet.add(1)
            val node2 = disjointSet.add(2)

            disjointSet.join(node1, node2)
            disjointSet.join(node1, node2)

            node1.find().rank shouldBe 1
        }

        test("find should return the root node of the subset") {
            val node1 = disjointSet.add(1)
            val node2 = disjointSet.add(2)

            disjointSet.join(node1, node2)

            node1.find() shouldBe node2.find()
        }

        test("find should return the node itself if it is the root node") {
            val node1 = disjointSet.add(1)

            node1.find() shouldBe node1
        }

        test("Node equals should return true if two nodes belong to the same subset") {
            val node1 = disjointSet.add(1)
            val node2 = disjointSet.add(2)

            disjointSet.join(node1, node2)

            node1 shouldBe node2
        }

        test("Node equals should return false if two nodes belong to different subsets") {
            val node1 = disjointSet.add(1)
            val node2 = disjointSet.add(2)

            node1 shouldNotBe node2
        }

        test("Node hashCode should return the same value for nodes in the same subset") {
            val node1 = disjointSet.add(1)
            val node2 = disjointSet.add(2)

            disjointSet.join(node1, node2)

            node1.hashCode() shouldBe node2.hashCode()
        }

        test("Node hashCode should return different values for nodes in different subsets") {
            val node1 = disjointSet.add(1)
            val node2 = disjointSet.add(2)

            node1.hashCode() shouldNotBe node2.hashCode()
        }

        test("Node toString should return the same value for nodes in the same subset") {
            val node1 = disjointSet.add(1)
            val node2 = disjointSet.add(2)

            disjointSet.join(node1, node2)

            node1.toString() shouldBe node2.toString()
        }

        test("Node toString should return different values for nodes in different subsets") {
            val node1 = disjointSet.add(1)
            val node2 = disjointSet.add(2)

            node1.toString() shouldNotBe node2.toString()
        }
    }
})