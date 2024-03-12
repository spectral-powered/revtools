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
import org.spectralpowered.revtools.asm.analysis.execution.Execution
import org.spectralpowered.revtools.asm.node.getMethod
import java.io.File

class ClassPoolTests : FunSpec({

    xtest("Loading Jar file") {
        val pool = ClassPool()
        pool.loadJar(File("../build/revtools/gamepack.jar"))
        pool.build()

        //pool.writeJar(File("../build/revtools/gamepack.out.jar"))

        println()
    }

    test("Check inherited method sets") {
        val pool = ClassPool()
        pool.loadJar(File("../build/revtools/gamepack.jar"))
        pool.build()

        val cls = pool.findClass("client")!!
        val method = cls.getMethod("init", "()V")!!

        val execution = Execution(pool)
        execution.populateEntryPoints()

        println()
    }
})