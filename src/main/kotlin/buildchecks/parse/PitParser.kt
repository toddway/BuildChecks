package buildchecks.parse

import buildchecks.model.FileMutations
import buildchecks.model.Mutation
import buildchecks.model.MutationData
import buildchecks.model.ParsedReport
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

/**
 * PIT (pitest) `mutations.xml`: a flat list of `<mutation>` elements under a `<mutations>` root.
 * `detected` and `status` are attributes on the element; `sourceFile`, `mutatedClass`, and
 * `lineNumber` are child text. The file path is derived the JaCoCo way — package from the mutated
 * class, filename from `sourceFile` — so mutation and coverage rows line up on the same path and can
 * be intersected with a diff (see MutationData.matching).
 */
class PitParser : ReportParser {
    override val format = "pit"

    // "mutations" is distinctive enough on its own — no other ingested format uses that root.
    override fun claims(content: String) = xmlRootElement(content) == "mutations"

    override fun parse(content: String): ParsedReport {
        val byPath = LinkedHashMap<String, MutableList<Mutation>>()
        parseXml(content, object : DefaultHandler() {
            var detected = false
            var status = ""
            var sourceFile: String? = null
            var mutatedClass: String? = null
            var line: Int? = null
            val text = StringBuilder()

            override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
                text.setLength(0)
                if (qName == "mutation") {
                    detected = attributes.getValue("detected") == "true"
                    status = attributes.getValue("status") ?: (if (detected) "KILLED" else "SURVIVED")
                    sourceFile = null
                    mutatedClass = null
                    line = null
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                text.appendRange(ch, start, start + length)
            }

            override fun endElement(uri: String, localName: String, qName: String) {
                when (qName) {
                    "sourceFile" -> sourceFile = text.toString().trim()
                    "mutatedClass" -> mutatedClass = text.toString().trim()
                    "lineNumber" -> line = text.toString().trim().toIntOrNull()
                    "mutation" -> {
                        val file = sourceFile ?: return
                        val place = line ?: return
                        val path = path(mutatedClass, file)
                        byPath.getOrPut(path) { mutableListOf() } += Mutation(place, status, detected)
                    }
                }
            }
        })
        return ParsedReport(mutation = MutationData(byPath.map { (path, mutations) -> FileMutations(path, mutations) }))
    }

    // "com.example.Foo" + "Foo.kt" -> "com/example/Foo.kt"; a nested class ("Foo$Bar") keeps the
    // same package; the default package (no dot) yields the bare filename.
    private fun path(mutatedClass: String?, sourceFile: String): String {
        val pkg = mutatedClass?.takeIf { it.contains('.') }?.substringBeforeLast('.') ?: ""
        return if (pkg.isEmpty()) sourceFile else "${pkg.replace('.', '/')}/$sourceFile"
    }
}
