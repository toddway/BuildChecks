package buildchecks.parse

import buildchecks.model.CoverageData
import buildchecks.model.FileCoverage
import buildchecks.model.LineCoverage
import buildchecks.model.ParsedReport
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

class JacocoParser : ReportParser {
    override val format = "jacoco"

    // Root element "report" is too generic on its own; the JaCoCo DOCTYPE is always present.
    override fun claims(content: String) =
        xmlRootElement(content) == "report" && content.take(8192).contains("-//JACOCO//DTD")

    override fun parse(content: String): ParsedReport {
        val files = mutableListOf<FileCoverage>()
        parseXml(content, object : DefaultHandler() {
            var packageName = ""
            var filePath: String? = null
            var lines = mutableListOf<LineCoverage>()

            override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
                when (qName) {
                    "package" -> packageName = attributes.getValue("name") ?: ""
                    "sourcefile" -> {
                        filePath = "$packageName/${attributes.getValue("name")}"
                        lines = mutableListOf()
                    }
                    "line" -> {
                        if (filePath == null) return
                        val line = attributes.getValue("nr")?.toIntOrNull() ?: return
                        val coveredInstructions = attributes.getValue("ci")?.toIntOrNull() ?: 0
                        val coveredBranches = attributes.getValue("cb")?.toIntOrNull() ?: 0
                        val missedBranches = attributes.getValue("mb")?.toIntOrNull() ?: 0
                        lines += LineCoverage(
                            line = line,
                            // JaCoCo records covered/missed instructions, not execution counts
                            hits = if (coveredInstructions > 0) 1 else 0,
                            coveredBranches = coveredBranches,
                            totalBranches = coveredBranches + missedBranches,
                        )
                    }
                }
            }

            override fun endElement(uri: String, localName: String, qName: String) {
                if (qName == "sourcefile") {
                    files += FileCoverage(filePath!!, lines)
                    filePath = null
                }
            }
        })
        return ParsedReport(coverage = CoverageData(files))
    }
}
