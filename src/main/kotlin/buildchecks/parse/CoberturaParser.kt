package buildchecks.parse

import buildchecks.model.CoverageData
import buildchecks.model.FileCoverage
import buildchecks.model.LineCoverage
import buildchecks.model.ParsedReport
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

class CoberturaParser : ReportParser {
    override val format = "cobertura"

    override fun claims(content: String) = xmlRootElement(content) == "coverage"

    override fun parse(content: String): ParsedReport {
        // Same file can appear as several <class> elements; merge by line, keeping the best hits.
        val files = LinkedHashMap<String, MutableMap<Int, LineCoverage>>()
        parseXml(content, object : DefaultHandler() {
            var filePath: String? = null
            var inMethods = false // <method><lines> repeat the class-level lines; count each line once

            override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
                when (qName) {
                    "class" -> filePath = attributes.getValue("filename")
                    "methods" -> inMethods = true
                    "line" -> {
                        val path = filePath
                        if (path == null || inMethods) return
                        val line = attributes.getValue("number")?.toIntOrNull() ?: return
                        val hits = attributes.getValue("hits")?.toLongOrNull() ?: 0
                        val (covered, total) = branchCounts(attributes.getValue("condition-coverage"))
                        val byLine = files.getOrPut(path) { LinkedHashMap() }
                        val next = LineCoverage(line, hits.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), covered, total)
                        val previous = byLine[line]
                        if (previous == null || next.hits > previous.hits) byLine[line] = next
                    }
                }
            }

            override fun endElement(uri: String, localName: String, qName: String) {
                when (qName) {
                    "methods" -> inMethods = false
                    "class" -> filePath = null
                }
            }
        })
        val coverage = CoverageData(files.map { (path, byLine) ->
            FileCoverage(path, byLine.values.sortedBy { it.line })
        })
        return ParsedReport(coverage = coverage)
    }

    // condition-coverage="50% (1/2)"
    private fun branchCounts(value: String?): Pair<Int, Int> {
        val match = value?.let { Regex("""\((\d+)/(\d+)\)""").find(it) } ?: return 0 to 0
        return match.groupValues[1].toInt() to match.groupValues[2].toInt()
    }
}
