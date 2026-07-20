package buildchecks.parse

import buildchecks.model.Finding
import buildchecks.model.Location
import buildchecks.model.ParsedReport
import buildchecks.model.Severity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SarifParser : ReportParser {
    override val format = "sarif"

    override fun claims(content: String): Boolean {
        val head = content.take(4096)
        if (!head.trimStart().startsWith("{")) return false
        return head.contains("sarif", ignoreCase = true) && content.contains("\"runs\"")
    }

    override fun parse(content: String): ParsedReport {
        val root = Json.parseToJsonElement(content).jsonObject
        val runs = root["runs"]?.jsonArray.orEmpty().map { it.jsonObject }
        val findings = runs.flatMap { run(it) }
        // The driver name is present even with zero results, so it keys the origin manifest
        // stably (V4-PLAN.md §5.5). Aggregated multi-tool SARIF is keyed by its first driver.
        val tool = runs.firstNotNullOfOrNull {
            it["tool"]?.jsonObject?.get("driver")?.jsonObject?.get("name")?.jsonPrimitive?.content
        }
        return ParsedReport(findings = findings, tool = tool)
    }

    private fun run(run: JsonObject): List<Finding> {
        val driver = run["tool"]?.jsonObject?.get("driver")?.jsonObject
        val tool = driver?.get("name")?.jsonPrimitive?.content ?: "sarif"
        val rules = driver?.get("rules")?.jsonArray.orEmpty().map { it.jsonObject }
        val defaultLevels = rules.mapNotNull { rule ->
            val id = rule["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val level = rule["defaultConfiguration"]?.jsonObject?.get("level")?.jsonPrimitive?.content
            level?.let { id to it }
        }.toMap()

        return run["results"]?.jsonArray.orEmpty().map { element ->
            val result = element.jsonObject
            val ruleId = result["ruleId"]?.jsonPrimitive?.content
                ?: result["ruleIndex"]?.jsonPrimitive?.intOrNull
                    ?.let { rules.getOrNull(it)?.get("id")?.jsonPrimitive?.content }
                ?: "unknown"
            val level = result["level"]?.jsonPrimitive?.content ?: defaultLevels[ruleId] ?: "warning"
            val locations = result["locations"]?.jsonArray.orEmpty().mapNotNull { location(it.jsonObject) }
            Finding(
                tool = tool,
                ruleId = ruleId,
                severity = severity(level),
                message = result["message"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: "",
                location = locations.firstOrNull(),
                relatedLocations = locations.drop(1),
            )
        }
    }

    private fun location(location: JsonObject): Location? {
        val physical = location["physicalLocation"]?.jsonObject ?: return null
        val uri = physical["artifactLocation"]?.jsonObject?.get("uri")?.jsonPrimitive?.content ?: return null
        val region = physical["region"]?.jsonObject
        return Location(
            path = uri.removePrefix("file://"),
            line = region?.get("startLine")?.jsonPrimitive?.intOrNull,
            column = region?.get("startColumn")?.jsonPrimitive?.intOrNull,
        )
    }

    private fun severity(level: String) = when (level) {
        "error" -> Severity.ERROR
        "warning" -> Severity.WARNING
        else -> Severity.INFO // "note", "none"
    }
}
