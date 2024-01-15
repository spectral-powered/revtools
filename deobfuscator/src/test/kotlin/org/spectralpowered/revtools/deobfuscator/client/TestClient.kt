@file:Suppress("DEPRECATION")

package org.spectralpowered.revtools.deobfuscator.client

import java.applet.Applet
import java.applet.AppletStub
import java.awt.Color
import java.awt.Dimension
import java.awt.GridLayout
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import javax.swing.JFrame

class TestClient(private val jarFile: File) {

    private val params = hashMapOf<String, String>()

    fun start() {
        fetchParams()

        val classLoader = URLClassLoader(arrayOf(jarFile.toURI().toURL()))
        val initClass = classLoader.loadClass(params["initial_class"]!!.replace(".class", ""))
        val applet = initClass.getDeclaredConstructor().newInstance() as Applet

        applet.background = Color.BLACK
        applet.layout = null
        applet.size = Dimension(params["applet_minwidth"]!!.toInt(), params["applet_minheight"]!!.toInt())
        applet.preferredSize = applet.size
        applet.setStub(applet.createStub())
        applet.init()

        val frame = JFrame("Test Client : ${jarFile.name}")
        frame.layout = GridLayout(1, 0)
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.add(applet)
        frame.pack()
        frame.minimumSize = frame.size
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }

    private fun fetchParams() {
        val lines = URL("http://oldschool1.runescape.com/jav_config.ws").readText().split("\n")
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
    }

    private fun Applet.createStub() = object : AppletStub {
        override fun getAppletContext() = null
        override fun isActive() = true
        override fun getCodeBase() = URL(params["codebase"])
        override fun getDocumentBase() = codeBase
        override fun appletResize(width: Int, height: Int) { this@createStub.resize(width, height) }
        override fun getParameter(key: String) = params[key]
    }
}