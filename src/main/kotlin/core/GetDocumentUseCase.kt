package core

import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

class GetDocumentUseCase(val file: File) {
    fun execute() : Document {
        val dbf = DocumentBuilderFactory.newInstance()
        val db = dbf.newDocumentBuilder()
        db.setEntityResolver { _, systemId ->
            if (systemId.contains("report.dtd")) {
                InputSource(StringReader(""))
            } else {
                null
            }
        }

        return db.parse(file)
    }
}