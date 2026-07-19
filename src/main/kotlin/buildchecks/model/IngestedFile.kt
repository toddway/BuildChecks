package buildchecks.model

data class IngestedFile(
    val path: String,
    val format: String,
    val lastModified: Long,
    val report: ParsedReport,
    val content: String = "",
    val toolReport: String? = null, // output-dir-relative link to the tool's own copied HTML report
)
