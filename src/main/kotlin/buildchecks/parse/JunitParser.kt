package buildchecks.parse

import buildchecks.model.ParsedReport
import buildchecks.model.TestResult
import buildchecks.model.TestStatus
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

class JunitParser : ReportParser {
    override val format = "junit"

    override fun claims(content: String) = xmlRootElement(content) in setOf("testsuite", "testsuites")

    override fun parse(content: String): ParsedReport {
        val tests = mutableListOf<TestResult>()
        parseXml(content, object : DefaultHandler() {
            val suites = ArrayDeque<String>()
            var current: TestResult? = null

            override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
                when (qName) {
                    "testsuite" -> suites.addLast(attributes.getValue("name") ?: "")
                    "testcase" -> current = TestResult(
                        suite = attributes.getValue("classname") ?: suites.lastOrNull() ?: "",
                        name = attributes.getValue("name") ?: "",
                        status = TestStatus.PASSED,
                        durationSeconds = attributes.getValue("time")?.toDoubleOrNull(),
                    )
                    "failure" -> mark(TestStatus.FAILED, attributes)
                    "error" -> mark(TestStatus.ERROR, attributes)
                    "skipped" -> mark(TestStatus.SKIPPED, attributes)
                }
            }

            override fun endElement(uri: String, localName: String, qName: String) {
                when (qName) {
                    "testsuite" -> suites.removeLastOrNull()
                    "testcase" -> {
                        current?.let { tests += it }
                        current = null
                    }
                }
            }

            fun mark(status: TestStatus, attributes: Attributes) {
                val case = current ?: return
                if (case.status == TestStatus.PASSED) {
                    current = case.copy(status = status, message = attributes.getValue("message"))
                }
            }
        })
        return ParsedReport(tests = tests)
    }
}
