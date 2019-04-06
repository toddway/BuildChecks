package core.usecase

import core.attr
import core.children
import core.entryChildrenSum
import core.isNotLessThan
import org.w3c.dom.Document
import org.w3c.dom.Node

open class GetLintSummaryUseCase(
        val documents: List<Document>,
        val maxViolations : Int? = null) : GetSummaryUseCase {
    override fun isSuccessful(): Boolean {
        return maxViolations?.isNotLessThan(asTotal()) ?: true
    }

    override fun key(): String {
        return "lint"
    }

    override fun value(): String? {
        return violationMap.toViolationSummary(maxViolations)
    }

    private val violationMap: Map<String?, List<Node>>? by lazy { documents.toViolationMap() }

    fun asTotal(): Int? {
        return violationMap?.entryChildrenSum()
    }
}

fun Map<String?, List<Node>>?.toViolationSummary(maxViolations: Int?) : String? {
    return this?.let {
        val violationTypes = flatMap { e -> listOf("${e.value.count()} ${e.key?.toLowerCase()}") }.toMutableList()
        var summary = "" + entryChildrenSum() + " violations " + "(" + violationTypes.joinToString(", ") + ")"
        maxViolations?.let { summary += ", threshold is $maxViolations" }
        return summary
    }
}

fun List<Document>.toViolationMap(): Map<String?, List<Node>>? {
    return if (isEmpty()) null
    else children(listOf("issue", "error", "duplication")).groupBy { it.deriveViolationKey() }
}

fun Node.deriveViolationKey() : String {
    return if (nodeName == "duplication") "clones"
    else attr("severity")?.let {
        when (it.toLowerCase()) {
            "fatal" -> "error"
            "informational" -> "info"
            else -> it.toLowerCase()
        }
    } ?: "unknown"
}
