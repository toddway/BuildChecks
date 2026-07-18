package buildchecks.parse

import buildchecks.model.ParsedReport

interface ReportParser {
    /** Format id shown in output, e.g. "sarif", "junit". */
    val format: String

    /** Content sniffing — never trust filenames. Must be cheap: look at the head, not the whole file. */
    fun claims(content: String): Boolean

    fun parse(content: String): ParsedReport
}
