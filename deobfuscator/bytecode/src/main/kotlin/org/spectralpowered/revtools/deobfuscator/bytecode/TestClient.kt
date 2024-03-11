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

@file:Suppress("removal", "DEPRECATION")

package org.spectralpowered.revtools.deobfuscator.bytecode

import java.applet.Applet
import java.applet.AppletContext
import java.applet.AppletStub
import java.awt.Color
import java.awt.Dimension
import java.awt.GridLayout
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import javax.swing.JFrame

class TestClient(private val gamepack: File) {

    private val params = hashMapOf<String, String>()

    fun start() {
        // Fetch jav config params.
        val lines = URL("http://oldschool1.runescape.com/jav_config.ws")
            .openConnection()
            .getInputStream()
            .bufferedReader()
            .readText()
            .split("\n")
        lines.forEach {
            var line = it
            if(line.startsWith("param=")) {
                line = line.substring(6)
            }
            val idx = line.indexOf("=")
            if(idx >= 0) {
                params[line.substring(0, idx)] = line.substring(idx + 1)
            }
        }

        // Load client applet
        val classLoader = URLClassLoader(arrayOf(gamepack.toURI().toURL()))
        val main = params["initial_class"]!!.replace(".class", "")
        val applet = classLoader.loadClass(main).newInstance() as Applet

        applet.background = Color.BLACK
        applet.layout = null
        applet.size = Dimension(params["applet_minwidth"]!!.toInt(), params["applet_minheight"]!!.toInt())
        applet.preferredSize = applet.size
        applet.createStub()
        applet.init()

        // Create frame
        val frame = JFrame("Test Client - ${gamepack.name}")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.layout = GridLayout(1, 0)
        frame.add(applet)
        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.minimumSize = frame.size
        frame.isVisible = true
    }

    private fun Applet.createStub() {
        val stub = object : AppletStub {
            override fun getAppletContext(): AppletContext? {
                return null
            }
            override fun getCodeBase(): URL {
                return URL(params["codebase"])
            }
            override fun getDocumentBase(): URL {
                return codeBase
            }
            override fun isActive(): Boolean {
                return true
            }
            override fun getParameter(name: String): String? {
                return params[name]
            }
            override fun appletResize(width: Int, height: Int) {
                this@createStub.size = Dimension(width, height)
            }
        }
        setStub(stub)
    }
}