package buildchecks.render

import buildchecks.model.CheckSummary
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/** Every ingested SARIF run combined into one file for GitHub code-scanning upload. */
class MergedSarif : Renderer {

    override val fileName = "merged.sarif"

    override fun render(summary: CheckSummary): String {
        val runs = summary.files
            .filter { it.format == "sarif" }
            .flatMap { Json.parseToJsonElement(it.content).jsonObject["runs"]?.jsonArray ?: JsonArray(emptyList()) }
        val merged = JsonObject(mapOf(
            "version" to JsonPrimitive("2.1.0"),
            "\$schema" to JsonPrimitive("https://json.schemastore.org/sarif-2.1.0.json"),
            "runs" to JsonArray(runs),
        ))
        return json.encodeToString(JsonObject.serializer(), merged)
    }

    private companion object {
        val json = Json { prettyPrint = true }
    }
}
