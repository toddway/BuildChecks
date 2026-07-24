package buildchecks.cli

import buildchecks.gate.GateConfig
import com.akuleshov7.ktoml.Toml
import java.io.File
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Plain config values (V4-PLAN.md §6); ktoml types never cross this boundary. */
data class Config(
    val reports: ReportsConfig = ReportsConfig(),
    val gates: GateConfig = GateConfig(),
    val git: GitConfig = GitConfig(),
)

data class ReportsConfig(
    val paths: List<String>? = null, // globs; null = the zero-config discovery set
    val outputDir: String = ReportDiscovery.DEFAULT_OUTPUT_DIR,
    val freshnessToleranceMinutes: Long = 15,
)

data class GitConfig(
    val baseRef: String? = null,
    val baselineFile: String = "buildchecks-baseline.txt",
)

/**
 * Loads `--config <path>`, else `buildchecks.toml` at the root, else all defaults
 * (empty file ≡ no file). `${VAR}` in string values is env-interpolated; an undefined
 * variable is an error, never a silent blank.
 */
fun loadConfig(
    explicit: File?,
    root: File,
    env: (String) -> String? = System::getenv,
): Config {
    if (explicit != null) require(explicit.isFile) { "config file not found: $explicit" }
    val file = explicit ?: File(root, "buildchecks.toml").takeIf { it.isFile } ?: return Config()
    val text = interpolate(file.readText(), env)
    val toml = try {
        Toml.decodeFromString(TomlSchema.serializer(), text)
    } catch (e: Exception) {
        throw IllegalArgumentException("invalid config $file: ${e.message}", e)
    }
    return toml.toConfig()
}

private fun interpolate(text: String, env: (String) -> String?): String =
    Regex("\\$\\{([A-Za-z_][A-Za-z0-9_]*)}").replace(text) { match ->
        val name = match.groupValues[1]
        env(name) ?: throw IllegalArgumentException("config references undefined environment variable \${$name}")
    }

@Serializable
private data class TomlSchema(
    val reports: ReportsToml = ReportsToml(),
    val gates: GatesToml = GatesToml(),
    val git: GitToml = GitToml(),
) {
    fun toConfig() = Config(
        reports = ReportsConfig(
            paths = reports.paths,
            outputDir = reports.outputDir,
            freshnessToleranceMinutes = reports.freshnessToleranceMinutes,
        ),
        gates = GateConfig(
            maxNewFindings = gates.maxNewFindings,
            ratchet = gates.ratchet,
            coverageTolerance = gates.coverageTolerance,
            minCoveragePercent = gates.minCoveragePercent,
            maxErrors = gates.maxErrors,
            maxWarnings = gates.maxWarnings,
            maxTestFailures = gates.maxTestFailures,
            minChangedLineCoverage = gates.minChangedLineCoverage,
            failOnSkippedGates = gates.failOnSkippedGates,
            requireBaseRef = gates.requireBaseRef,
        ),
        git = GitConfig(
            baseRef = git.baseRef,
            baselineFile = git.baselineFile,
        ),
    )
}

@Serializable
private data class ReportsToml(
    val paths: List<String>? = null,
    @SerialName("output_dir") val outputDir: String = ReportDiscovery.DEFAULT_OUTPUT_DIR,
    @SerialName("freshness_tolerance_minutes") val freshnessToleranceMinutes: Long = 15,
)

@Serializable
private data class GatesToml(
    @SerialName("max_new_findings") val maxNewFindings: Int = 0,
    val ratchet: Boolean = true,
    @SerialName("coverage_tolerance") val coverageTolerance: Double = 0.1,
    @SerialName("min_coverage_percent") val minCoveragePercent: Double? = null,
    @SerialName("max_errors") val maxErrors: Int? = null,
    @SerialName("max_warnings") val maxWarnings: Int? = null,
    @SerialName("max_test_failures") val maxTestFailures: Int? = null,
    @SerialName("min_changed_line_coverage") val minChangedLineCoverage: Int? = null,
    @SerialName("fail_on_skipped_gates") val failOnSkippedGates: Boolean = false,
    @SerialName("require_base_ref") val requireBaseRef: Boolean = false,
)

@Serializable
private data class GitToml(
    @SerialName("base_ref") val baseRef: String? = null,
    @SerialName("baseline_file") val baselineFile: String = "buildchecks-baseline.txt",
)
