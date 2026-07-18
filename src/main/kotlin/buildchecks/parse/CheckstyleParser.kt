package buildchecks.parse

import buildchecks.model.Finding
import buildchecks.model.Location
import buildchecks.model.ParsedReport
import buildchecks.model.Severity
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

class CheckstyleParser : ReportParser {
    override val format = "checkstyle"

    override fun claims(content: String) = xmlRootElement(content) == "checkstyle"

    override fun parse(content: String): ParsedReport {
        val findings = mutableListOf<Finding>()
        parseXml(content, object : DefaultHandler() {
            var filePath: String? = null

            override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
                when (qName) {
                    "file" -> filePath = attributes.getValue("name")
                    "error" -> findings += Finding(
                        tool = format,
                        ruleId = attributes.getValue("source") ?: "unknown",
                        severity = when (attributes.getValue("severity")) {
                            "error" -> Severity.ERROR
                            "info" -> Severity.INFO
                            else -> Severity.WARNING
                        },
                        message = attributes.getValue("message") ?: "",
                        location = filePath?.let {
                            Location(
                                path = it,
                                line = attributes.getValue("line")?.toIntOrNull(),
                                column = attributes.getValue("column")?.toIntOrNull(),
                            )
                        },
                    )
                }
            }

            override fun endElement(uri: String, localName: String, qName: String) {
                if (qName == "file") filePath = null
            }
        })
        return ParsedReport(findings = findings)
    }
}
