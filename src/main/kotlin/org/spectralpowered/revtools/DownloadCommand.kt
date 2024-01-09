package org.spectralpowered.revtools

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import org.jsoup.Jsoup
import java.io.File
import java.util.Properties

class DownloadCommand : CliktCommand(
    name = "download",
    help = "Downloads the latest or specific revision gamepack (client.jar) for Old School RuneScape"
) {

    private val jarFile by argument(
        name = "jar-save-path",
        help = "Path to save the gamepack jar file to"
    ).file(canBeDir = false).validate { it.extension === "jar" }

    private val revision by option(
        "-r", "--revision",
        help = "Custom revision number of gamepack to download or 'latest' for the live jagex revision. (default: latest)",
    ).default("latest")

    override fun run() {
        println("Downloading Gamepack...")

        val gamepack = when(revision) {
            "latest" -> Gamepack.Jagex()
            "previous", "prev", "last" -> {
                val latestGamepack = Gamepack.Jagex()
                Gamepack.Custom(latestGamepack.javConfig["param_25"].toInt() - 1)
            }
            else -> Gamepack.Custom(revision.toInt())
        }

        if(gamepack is Gamepack.Jagex) {
            println("Gamepack Revision: ${gamepack.javConfig["param_25"]}")
        } else if(gamepack is Gamepack.Custom) {
            println("Gamepack Revision: ${gamepack.revision}")
        }

        if(jarFile.exists()) {
            jarFile.deleteRecursively()
        }
        if(jarFile.parentFile != null && jarFile.parentFile.isDirectory) {
            jarFile.parentFile.mkdirs()
        }

        gamepack.save(jarFile)
        println("Successfully downloaded gamepack jar to file: '${jarFile.path}'")
    }
}

class JavConfig(val url: String = "https://oldschool.runescape.com/jav_config.ws") {
    val properties: Properties by lazy {
        Properties().also { it.load(
            Jsoup.connect(url)
                .get()
                .wholeText()
                .replace("msg=", "msg_")
                .replace("param=", "param_")
                .byteInputStream()
        ) }
    }

    operator fun get(key: String) = properties.getProperty(key) ?: error("No property $key in jav_config.")
}

sealed class Gamepack(
    private val bytes: ByteArray,
    private val initialClass: String
) {
    fun save(file: File) {
        if(file.exists()) file.deleteRecursively()
        if(file.parentFile != null) file.parentFile.mkdirs()
        file.createNewFile()
        file.outputStream().use { output ->
            output.write(bytes)
        }
    }

    class Jagex(val javConfig: JavConfig = JavConfig()) : Gamepack(
        Jsoup.connect(javConfig["codebase"] + javConfig["initial_jar"])
            .maxBodySize(0)
            .ignoreContentType(true)
            .execute()
            .bodyAsBytes(),
        javConfig["initial_class"]
    )

    class Custom(val revision: Int) : Gamepack(
        Jsoup.connect("https://github.com/runetech/osrs-gamepacks/raw/master/gamepacks/osrs-$revision.jar")
            .maxBodySize(0)
            .ignoreContentType(true)
            .execute()
            .bodyAsBytes(),
        "client.class"
    )
}