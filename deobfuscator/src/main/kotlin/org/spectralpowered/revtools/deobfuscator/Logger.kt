package org.spectralpowered.revtools.deobfuscator

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal

object Logger {

    enum class Level(val id: Int) {
        DEBUG(0),
        INFO(1),
        WARN(2),
        ERROR(3),
        SEVERE(4),
        NONE(5)
    }

    private val console = Terminal(AnsiLevel.TRUECOLOR)

    var level = Level.INFO

    fun debug(message: String) {
        if(level.id > Level.DEBUG.id) return
        console.println(brightCyan(bold("DEBUG")) + " " + reset(gray("-")) + " " + reset(message))
    }

    fun info(message: String) {
        if(level.id > Level.INFO.id) return
        console.println(brightGreen(bold("INFO")) + " " + reset(gray("-")) + " " + reset(message))
    }

    fun warn(message: String) {
        if(level.id > Level.WARN.id) return
        console.println(brightYellow(bold("WARN")) + " " + reset(gray("-")) + " " + brightWhite(message))
    }

    fun error(message: String) {
        if(level.id > Level.ERROR.id) return
        console.println(brightRed(bold("ERROR")) + " " + reset(gray("-")) + " " + brightWhite(message))
    }

    fun severe(message: String) {
        if(level.id > Level.SEVERE.id) return
        console.println((red on white).bg(bold("SEVERE")) + " " + reset(gray("-")) + " " + brightWhite(message))
    }

    fun error(message: String, throwable: Throwable) {
        error(message)
        throwable.printStackTrace()
    }

    fun severe(message: String, throwable: Throwable) {
        severe(message)
        throwable.printStackTrace()
    }
}