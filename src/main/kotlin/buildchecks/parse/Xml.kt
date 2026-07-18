package buildchecks.parse

import java.io.StringReader
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler

/** Name of the document's root element (namespace prefix stripped), or null if not XML-like. */
internal fun xmlRootElement(content: String): String? {
    val head = content.take(8192)
    var i = 0
    while (true) {
        val open = head.indexOf('<', i)
        if (open < 0 || open + 1 >= head.length) return null
        i = when {
            head.startsWith("<?", open) -> head.indexOf("?>", open).let { if (it < 0) return null else it + 2 }
            head.startsWith("<!--", open) -> head.indexOf("-->", open).let { if (it < 0) return null else it + 3 }
            head.startsWith("<!", open) -> head.indexOf('>', open).let { if (it < 0) return null else it + 1 }
            else -> {
                val name = head.substring(open + 1).takeWhile { !it.isWhitespace() && it != '>' && it != '/' }
                return name.substringAfter(':').ifEmpty { null }
            }
        }
    }
}

/** SAX parse that never fetches external DTDs/entities (JaCoCo and Cobertura reports declare them). */
internal fun parseXml(content: String, handler: DefaultHandler) {
    val factory = SAXParserFactory.newInstance().apply {
        setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    }
    factory.newSAXParser().parse(InputSource(StringReader(content)), handler)
}
