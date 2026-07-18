package buildchecks.parse

import buildchecks.model.CoverageData
import buildchecks.model.FileCoverage
import buildchecks.model.LineCoverage
import buildchecks.model.ParsedReport

class LcovParser : ReportParser {
    override val format = "lcov"

    override fun claims(content: String): Boolean {
        val first = content.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: return false
        return first.startsWith("TN:") || first.startsWith("SF:")
    }

    override fun parse(content: String): ParsedReport {
        val files = mutableListOf<FileCoverage>()
        var path: String? = null
        // c8 and merged reports repeat DA records for a line; execution counts add up
        var hits = LinkedHashMap<Int, Long>()
        var branches = LinkedHashMap<Int, IntArray>() // [covered, total]

        fun flush() {
            val current = path ?: return
            files += FileCoverage(current, hits.map { (line, count) ->
                val branch = branches[line]
                LineCoverage(
                    line = line,
                    hits = count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                    coveredBranches = branch?.get(0) ?: 0,
                    totalBranches = branch?.get(1) ?: 0,
                )
            })
            path = null
            hits = LinkedHashMap()
            branches = LinkedHashMap()
        }

        for (raw in content.lineSequence()) {
            val record = raw.trim()
            when {
                record.startsWith("SF:") -> {
                    flush()
                    path = record.removePrefix("SF:")
                }
                record.startsWith("DA:") -> {
                    val parts = record.removePrefix("DA:").split(',')
                    val line = parts.getOrNull(0)?.toIntOrNull() ?: continue
                    hits.merge(line, parts.getOrNull(1)?.toLongOrNull() ?: 0, Long::plus)
                }
                record.startsWith("BRDA:") -> {
                    val parts = record.removePrefix("BRDA:").split(',')
                    val line = parts.getOrNull(0)?.toIntOrNull() ?: continue
                    val taken = parts.getOrNull(3)
                    val counts = branches.getOrPut(line) { intArrayOf(0, 0) }
                    counts[1]++
                    if (taken != null && taken != "-" && (taken.toLongOrNull() ?: 0) > 0) counts[0]++
                }
                record == "end_of_record" -> flush()
            }
        }
        flush()
        return ParsedReport(coverage = CoverageData(files))
    }
}
