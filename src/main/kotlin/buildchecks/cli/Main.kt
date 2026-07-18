package buildchecks.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = BuildChecks()
    .subcommands(Check(), Baseline())
    .main(args)

class BuildChecks : CliktCommand(name = "buildchecks") {
    override fun run() = Unit
}

class Check : CliktCommand(name = "check") {
    override fun run() {
        echo("check: not implemented yet — see V4-PLAN.md, phases 1-5")
    }
}

class Baseline : CliktCommand(name = "baseline") {
    override fun run() {
        echo("baseline: not implemented yet — see V4-PLAN.md, phase 2")
    }
}
