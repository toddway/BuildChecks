package buildchecks.model

enum class Severity { ERROR, WARNING, INFO }

data class Location(
    val path: String,
    val line: Int? = null,
    val column: Int? = null,
)

data class Finding(
    val tool: String,
    val ruleId: String,
    val severity: Severity,
    val message: String,
    val location: Location? = null,
    val relatedLocations: List<Location> = emptyList(),
    val snippet: String? = null,
    val duplicatedTokens: Int? = null,
)
