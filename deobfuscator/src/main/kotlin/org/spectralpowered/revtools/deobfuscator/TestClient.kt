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

@file:Suppress("DEPRECATION", "removal")

package org.spectralpowered.revtools.deobfuscator

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
import kotlin.reflect.full.createInstance

class TestClient(private val gamepackJar: File) {

    private companion object {
        const val JAGEX_URL = "http://oldschool1.runescape.com/"
    }

    private val params = hashMapOf<String, String>()

    fun start() {
        // Fetch jav_config params
        val lines = URL(JAGEX_URL+"jav_config.ws")
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
            val idx = line.indexOf('=')
            if(idx >= 0) {
                params[line.substring(0, idx)] = line.substring(idx + 1)
            }
        }

        // Load gamepack jar classes
        val classLoader = URLClassLoader(arrayOf(gamepackJar.toURI().toURL()), TestClient::class.java.classLoader)
        val main = params["initial_class"]!!.replace(".class", "")
        val applet = classLoader.loadClass(main).newInstance() as Applet

        // Configure applet
        applet.background = Color.BLACK
        applet.size = Dimension(params["applet_minwidth"]!!.toInt(), params["applet_minheight"]!!.toInt())
        applet.preferredSize = applet.size
        applet.layout = null
        applet.createStub()
        applet.isVisible = true
        applet.init()

        // Create JFRAME window
        val frame = JFrame("Test Client - ${gamepackJar.name}")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.layout = GridLayout(1, 0)
        frame.add(applet)
        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
        frame.minimumSize = frame.size
    }

    private fun Applet.createStub() {
        val stub = object : AppletStub {
            override fun isActive(): Boolean = true
            override fun getAppletContext(): AppletContext? = null
            override fun getParameter(name: String): String? = params[name]
            override fun getCodeBase(): URL = URL(params["codebase"])
            override fun getDocumentBase(): URL = URL(params["codebase"])
            override fun appletResize(width: Int, height: Int) { this@createStub.size = Dimension(width, height) }
        }
        setStub(stub)
    }
}