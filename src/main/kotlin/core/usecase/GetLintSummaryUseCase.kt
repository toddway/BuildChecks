package core.usecase

import core.attr
import core.children
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList

open class GetLintSummaryUseCase(val documents: List<Document>) : GetSummaryUseCase {
    override fun keyString(): String {
        return "l"
    }

    override fun summaryString(): String? {
        return asString()
    }

    var suffix = " Lint issues "
    var tagName = "issue"

    fun asString(): String? {
        return severityMap?.let {
            val countsList = it.flatMap { entry -> listOf("${entry.value.count()} ${entry.key?.toLowerCase()}s") }
            return "" + asTotal() + suffix + "(" + countsList.joinToString(", ") + ")"
        }
    }

    val severityMap: Map<String?, List<Node>>? by lazy {
        if (documents.isEmpty()) null
        else documents.toSeverityMap(tagName)
    }

    fun asTotal(): Int? {
        return severityMap?.totalListItemsInMap()
    }
}

fun Document.toSeverityMap(tagName : String): Map<String?, List<Node>> {
    val nodeList : NodeList? = getElementsByTagName(tagName)
    return nodeList?.children()?.groupBy { it.attr("severity") } ?: mapOf()
}

fun List<Document>.toSeverityMap(tagName : String): Map<String?, List<Node>> {
    val m = mutableMapOf<String?, List<Node>>()
    map { it.toSeverityMap(tagName) }.forEach { m.putAll(it) }
    return m
}


fun <T, V> Map<V?, List<T>>.totalListItemsInMap(): Int {
    return flatMap { entry -> listOf(entry.value.count()) }.sum()
}
