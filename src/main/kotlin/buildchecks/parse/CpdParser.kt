package buildchecks.parse

import buildchecks.model.Finding
import buildchecks.model.Location
import buildchecks.model.ParsedReport
import buildchecks.model.Severity
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

class CpdParser : ReportParser {
    override val format = "cpd"

    override fun claims(content: String) = xmlRootElement(content) == "pmd-cpd"

    override fun parse(content: String): ParsedReport {
        val findings = mutableListOf<Finding>()
        parseXml(content, object : DefaultHandler() {
            var duplicatedLines = 0
            var tokens = 0
            var locations = mutableListOf<Location>()
            var fragment: StringBuilder? = null
            var fragmentText: String? = null

            override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
                when (qName) {
                    "duplication" -> {
                        duplicatedLines = attributes.getValue("lines")?.toIntOrNull() ?: 0
                        tokens = attributes.getValue("tokens")?.toIntOrNull() ?: 0
                        locations = mutableListOf()
                        fragmentText = null
                    }
                    "file" -> locations += Location(
                        path = attributes.getValue("path") ?: "",
                        line = attributes.getValue("line")?.toIntOrNull(),
                    )
                    "codefragment" -> fragment = StringBuilder()
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                fragment?.append(ch, start, length)
            }

            override fun endElement(uri: String, localName: String, qName: String) {
                when (qName) {
                    "codefragment" -> {
                        fragmentText = fragment?.toString()
                        fragment = null
                    }
                    "duplication" -> findings += Finding(
                        tool = format,
                        ruleId = "duplicated-code",
                        severity = Severity.WARNING,
                        message = "$duplicatedLines duplicated lines ($tokens tokens) in ${locations.size} places",
                        location = locations.firstOrNull(),
                        relatedLocations = locations.drop(1),
                        snippet = fragmentText,
                        duplicatedTokens = tokens,
                    )
                }
            }
        })
        return ParsedReport(findings = findings)
    }
}
