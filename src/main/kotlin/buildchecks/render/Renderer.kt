package buildchecks.render

import buildchecks.model.CheckSummary

/** A file written to the output directory on every check run. */
interface Renderer {
    val fileName: String
    fun render(summary: CheckSummary): String
}
