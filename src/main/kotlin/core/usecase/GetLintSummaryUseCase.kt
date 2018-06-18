package core.usecase

import core.attr
import core.children
import core.isNotLessThan
import org.w3c.dom.Document
import org.w3c.dom.Node

open class GetLintSummaryUseCase(
        val documents: List<Document>,
        val maxLintViolations : Int? = null) : GetSummaryUseCase {
    override fun isSuccessful(): Boolean {
        return maxLintViolations?.isNotLessThan(asTotal()) ?: true
    }

    override fun key(): String {
        return "lint"
    }

    override fun summary(): String? {
        return asString()
    }

    fun asString(): String? {
        return severityMap?.let {
            val detailList = it.flatMap { entry ->
                listOf("${entry.value.count()} ${entry.key?.toLowerCase()}")
            }.toMutableList()
            var summary = "" + asTotal() + " rule violations " + "(" + detailList.joinToString(", ") + ")"
            maxLintViolations?.let { summary += ", threshold is $maxLintViolations" }
            return summary
        }
    }

    val severityMap: Map<String?, List<Node>>? by lazy {
        if (documents.isEmpty()) null
        else documents.children(listOf("issue", "error")).toSeverityMap()
    }

    fun asTotal(): Int? {
        return severityMap?.totalListItemsInMap()
    }
}

fun String.normalizeLabels() : String {
    return when (this.toLowerCase()) {
        "fatal" -> "error"
        "informational" -> "info"
        else -> this.toLowerCase()
    }
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

fun Iterable<Node>.toSeverityMap(): Map<String?, List<Node>> {
    return groupBy { it.attr("severity")?.normalizeLabels() }
}

fun <T, V> Map<V?, List<T>>.totalListItemsInMap(): Int {
    return flatMap { entry -> listOf(entry.value.count()) }.sum()
}
