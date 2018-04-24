package core

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

class GetDocumentUseCase(val file: File) {
    fun execute() : Document {
        return file.toDocument()
    }
}


fun NodeList.children() = object : Iterable<Node> {
    override fun iterator() = object : Iterator<Node> {
        var index = 0
        override fun hasNext() = index < length
        override fun next() = item(index++)
    }
}

fun Node.attr(name : String): String? {
    return (this as Element).getAttribute(name)
}