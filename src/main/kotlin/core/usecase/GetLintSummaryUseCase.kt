package core.usecase

import core.*
import core.entity.BuildConfig
import org.w3c.dom.Document
import org.w3c.dom.Node

open class GetLintSummaryUseCase(
        val documents: List<Document>,
        val maxViolations : Int? = null) : GetSummaryUseCase {
    override fun isSuccessful(): Boolean = maxViolations?.isNotLessThan(asTotal()) ?: true
    override fun key(): String = "lint"
    override fun value(): String? = violationMap.toViolationSummary(maxViolations, documents.size)
    private val violationMap: Map<String?, List<Node>>? by lazy { documents.toViolationMap() }
    fun asTotal(): Int? = violationMap?.entryChildrenSum()
    companion object
}

fun Map<String?, List<Node>>?.toViolationSummary(maxViolations: Int?, numDocuments : Int) : String? {
    return this?.let {
        val violationTypes = flatMap { e -> listOf("${e.value.count()} ${e.key?.toLowerCase()}") }.toMutableList()
        val sum = entryChildrenSum()
        var summary = ""
        summary +=
                if (sum == 0) "0 violations"
                else "$sum violations " + "(" + violationTypes.joinToString(", ") + ")"
        maxViolations?.let { summary += ", max is $maxViolations" }
        return summary
    }
}

fun List<Document>.toViolationMap(): Map<String?, List<Node>>? {
    return if (isEmpty()) null
    else children(listOf("issue", "error", "duplication", "violation")).groupBy { it.deriveViolationKey() }
}

fun Node.deriveViolationKey() : String {
    return if (nodeName == "duplication") "clones"
    else if (nodeName == "violation") "error"
    else attr("severity")?.let {
        when (it.toLowerCase()) {
            "fatal" -> "error"
            "informational" -> "info"
            else -> it.toLowerCase()
        }
    } ?: "unknown"
}

fun List<Document>.buildLint(maxLintViolations : Int?) : GetLintSummaryUseCase {
    return GetLintSummaryUseCase(this, maxLintViolations)
}