package org.spectralpowered.revtools

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.HelpFormatter
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.mordant.rendering.*
import com.github.ajalt.mordant.table.ColumnWidth
import com.github.ajalt.mordant.table.verticalLayout
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Padding
import com.github.ajalt.mordant.widgets.Panel

fun main(args: Array<String>) {
    val theme = Theme {
        styles["info"] = TextColors.green
        styles["warning"] = TextColors.yellow
        styles["error"] = TextColors.red
        styles["danger"] = TextColors.brightRed
        styles["muted"] = TextColors.gray
        flags["markdown.code.block.border"] = false
    }

    RevToolsCommand(Terminal(theme = theme))
        .subcommands(
            DownloadGamepackCommand()
        )
        .main(args)
}

class PanelHelpFormatter(context: Context) : MordantHelpFormatter(context) {
    // You can override which styles are used for each part of the output.
    // If you want to change the color of the styles themselves, you can set them in the terminal's
    // theme (see the main function below).
    override fun styleSectionTitle(title: String): String = theme.style("muted")(title)

    // Print section titles like "Options" instead of "Options:"
    override fun renderSectionTitle(title: String): String = title

    // Print metavars like INT instead of <int>
    override fun normalizeParameter(name: String): String = name.uppercase()

    // Print option values like `--option VALUE instead of `--option=VALUE`
    override fun renderAttachedOptionValue(metavar: String): String = " $metavar"

    // Put each parameter section in its own panel
    override fun renderParameters(
        parameters: List<HelpFormatter.ParameterHelp>,
    ): Widget = verticalLayout {
        width = ColumnWidth.Auto
        for (section in collectParameterSections(parameters)) {
            cell(
                Panel(
                    section.content,
                    section.title,
                    expand = false,
                    padding = Padding(1, 5, 1, 1),
                    titleAlign = TextAlign.LEFT,
                    borderType = BorderType.HEAVY_HEAD_FOOT,
                    borderStyle = theme.style("muted")
                )
            )
        }
    }
}

class RevToolsCommand(terminal: Terminal) : NoOpCliktCommand(
    name = "revtools",
    help = "RevTools Command Line Tools",
    printHelpOnEmptyArgs = true
) {
    init {
        context {
            this.terminal = terminal
            helpFormatter = { PanelHelpFormatter(it) }
        }
    }

}