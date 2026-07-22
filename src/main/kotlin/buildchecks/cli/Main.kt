package buildchecks.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import java.io.File

// Bare `buildchecks` runs the default command, check (V4-PLAN.md §6).
fun main(args: Array<String>) = BuildChecks()
    .subcommands(Check(), Baseline())
    .main(if (args.isEmpty()) arrayOf("check") else args)

class BuildChecks : CliktCommand(name = "buildchecks") {
    override fun help(context: Context) = "Aggregates code-analysis and test/coverage reports into one gated summary."
    override fun run() = Unit
}

class Check : CliktCommand(name = "check") {
    override fun help(context: Context) = "Ingest reports, evaluate gates, write the output files (default command)."
    private val configPath by option("--config", help = "config file (default: buildchecks.toml at the root)")
        .file()
    private val outputDir by option("--output-dir", help = "where reports are written")
    private val baseRef by option("--base-ref", help = "git ref to diff changed-line coverage against")
    private val open by option("--open", help = "open index.html when done").flag()
    private val verbose by option("--verbose", help = "print discovery and config detail").flag()

    override fun run() {
        val root = File(".").canonicalFile
        var reportDir: File? = null
        val code = runCatchingIo {
            val config = configure(configPath, outputDir, root)
            reportDir = File(root, config.reports.outputDir)
            runCheck(root, config, baseRef, verbose, echo = ::echo)
        }
        // --open is a no-op under CI so a consumer can pass it unconditionally: opening a browser
        // on a build agent is never wanted, and the CI env var is the reliable signal for that.
        if (open && System.getenv("CI").isNullOrBlank()) reportDir?.let { openInBrowser(File(it, "index.html")) }
        exit(code)
    }
}

class Baseline : CliktCommand(name = "baseline") {
    override fun help(context: Context) = "Snapshot current findings and coverage as the committed baseline."
    private val configPath by option("--config", help = "config file (default: buildchecks.toml at the root)")
        .file()
    private val verbose by option("--verbose", help = "print discovery and config detail").flag()

    override fun run() {
        val root = File(".").canonicalFile
        exit(runCatchingIo { runBaseline(root, configure(configPath, null, root), verbose) { echo(it) } })
    }
}

private fun configure(configPath: File?, outputDir: String?, root: File): Config {
    val config = loadConfig(configPath, root)
    return if (outputDir == null) config
    else config.copy(reports = config.reports.copy(outputDir = outputDir))
}

// Gate failures exit 1; config/IO errors exit 2 (V4-PLAN.md §4).
private fun CliktCommand.runCatchingIo(verb: () -> Int): Int = try {
    verb()
} catch (e: Exception) {
    echo("error: ${e.message ?: e::class.simpleName}", err = true)
    2
}

private fun exit(code: Int) {
    if (code != 0) throw ProgramResult(code)
}

private fun openInBrowser(file: File) {
    val os = System.getProperty("os.name").lowercase()
    val command = when {
        "mac" in os -> listOf("open", file.absolutePath)
        "win" in os -> listOf("cmd", "/c", "start", "", file.absolutePath)
        else -> listOf("xdg-open", file.absolutePath)
    }
    runCatching { ProcessBuilder(command).start() }
}
