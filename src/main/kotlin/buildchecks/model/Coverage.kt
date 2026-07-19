package buildchecks.model

data class CoverageData(val files: List<FileCoverage>) {
    val linesTotal: Int get() = files.sumOf { it.lines.size }
    val linesCovered: Int get() = files.sumOf { it.linesCovered }
    val linePercent: Double? get() = if (linesTotal == 0) null else 100.0 * linesCovered / linesTotal
}

data class FileCoverage(val path: String, val lines: List<LineCoverage>) {
    val linesCovered: Int get() = lines.count { it.hits > 0 }
}

data class LineCoverage(
    val line: Int,
    val hits: Int,
    val coveredBranches: Int = 0,
    val totalBranches: Int = 0,
)
