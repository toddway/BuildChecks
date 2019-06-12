package core

import core.entity.Log
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

fun File.toDocument() : Document {
    val dbf = DocumentBuilderFactory.newInstance()
    val db = dbf.newDocumentBuilder()
    db.setEntityResolver { _, systemId ->
        if (systemId.contains(".dtd")) {
            InputSource(StringReader(""))
        } else {
            null
        }
    }

    return db.parse(this)
}

fun Iterable<String>.toDocuments(log : Log? = null) = toFiles(log).map { it.toDocument() }

fun NodeList.children() = object : Iterable<Node> {
    override fun iterator() = object : Iterator<Node> {
        var index = 0
        override fun hasNext() = index < length
        override fun next() = item(index++)
    }
}

fun Node.attr(name : String): String? {
    if (this.nodeType != Node.ELEMENT_NODE) return null
    return (this as Element).getAttribute(name)
}

fun List<Document>.children(childNames: List<String>) : Iterable<Node> {
    return flatMap { it.children(childNames) }
}

fun Document.children(childNames : List<String>) : Iterable<Node> {
    var nodes = mutableListOf<Node>().asIterable()
    childNames.forEach {
        getElementsByTagName(it)?.let { nodes += it.children() }
    }
    return nodes
}

