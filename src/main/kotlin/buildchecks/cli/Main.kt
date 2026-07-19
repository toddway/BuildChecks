package buildchecks.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import java.io.File

fun main(args: Array<String>) = BuildChecks()
    .subcommands(Check(), Baseline())
    .main(args)

class BuildChecks : CliktCommand(name = "buildchecks") {
    override fun run() = Unit
}

class Check : CliktCommand(name = "check") {
    override fun run() = exit(runCatchingIo { runCheck(File("."), echo = ::echo) })
}

class Baseline : CliktCommand(name = "baseline") {
    override fun run() = exit(runCatchingIo { runBaseline(File(".")) { echo(it) } })
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
