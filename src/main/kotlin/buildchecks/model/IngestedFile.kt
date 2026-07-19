package buildchecks.model

data class IngestedFile(
    val path: String,
    val format: String,
    val lastModified: Long,
    val report: ParsedReport,
)
