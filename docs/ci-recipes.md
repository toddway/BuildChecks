# CI recipes

BuildChecks stays out of the platform-API business: it writes `summary.json` (a small, stable,
versioned contract) and exits non-zero on failure. Everything below is glue *you* own that reads
those two things. No BuildChecks code talks to any CI.

It also writes **`summary.txt`** — a single-line, ready-to-post gate headline it composes itself
(e.g. `all gates passed · coverage 94.08% · 108 tests, 0 failed · 0 new findings`), kept short
enough for a commit-status description. Post it verbatim with `"$(cat summary.txt)"` instead of
assembling a sentence from `summary.json` in every project.

`summary.json` shape (the fields these recipes use):

```json
{
  "schemaVersion": 1,
  "passed": false,
  "coveragePercent": 61.06,
  "gates": [ { "gate": "coverage", "status": "FAILED", "detail": "…" } ]
}
```

## GitHub Actions

The exit code already fails the job. Add the human summary, attach the full report, and post a
`buildchecks` commit status whose description is BuildChecks' own `summary.txt`:

```yaml
- uses: toddway/BuildChecks@v4.0.0        # or: run java -jar …
  id: buildchecks

# Attach the browsable HTML report + its copied sub-reports as a downloadable artifact.
# (Artifacts inherit repo visibility, so this works identically for private and public repos.)
- name: Upload BuildChecks report
  if: always()
  id: report
  uses: actions/upload-artifact@v4
  with:
    name: buildchecks-report
    path: build/reports/buildchecks

- name: Job summary
  if: always()
  run: |
    cat build/reports/buildchecks/summary.md >> "$GITHUB_STEP_SUMMARY"
    echo "" >> "$GITHUB_STEP_SUMMARY"
    echo "📦 **[Download full HTML report](${{ steps.report.outputs.artifact-url }})** (zip — open \`index.html\`)" >> "$GITHUB_STEP_SUMMARY"

# A `buildchecks` status on the commit: pass/fail + the one-line summary; "Details" → the report.
- name: Commit status
  if: always()
  env:
    GH_TOKEN: ${{ github.token }}
  run: |
    state="${{ steps.buildchecks.outcome == 'success' && 'success' || 'failure' }}"
    gh api -X POST "repos/${{ github.repository }}/statuses/${{ github.event.pull_request.head.sha || github.sha }}" \
      -f state="$state" -f context="buildchecks" \
      -f description="$(cat build/reports/buildchecks/summary.txt)" \
      -f target_url="${{ steps.report.outputs.artifact-url }}"
```

Needs `permissions: { statuses: write }`. Fork PRs get a read-only token, so the status step is
skipped there — the job's own pass/fail check still covers them. For code scanning, also upload
`merged.sarif` with `github/codeql-action/upload-sarif`. For PR comments, feed `summary.md` to any
sticky-comment action.

## GitHub commit status from any CI (Bitrise, Jenkins, CircleCI…)

When you're **not** on GitHub Actions, post a commit status with a few lines of `curl`, reading
state and description straight from `summary.json`. The token lives in your CI's secret store.

```bash
STATE=$(jq -r 'if .passed then "success" else "failure" end' build/reports/buildchecks/summary.json)
DESC=$(jq -r '[.gates[] | select(.status=="FAILED") | .gate] | join(", ") | if .=="" then "all gates passed" else "failed: " + . end' build/reports/buildchecks/summary.json)

curl -sf -X POST \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  "https://api.github.com/repos/$OWNER/$REPO/statuses/$COMMIT_SHA" \
  -d "$(jq -n --arg s "$STATE" --arg d "$DESC" \
        '{state:$s, context:"buildchecks", description:$d}')"
```

## Post from Gradle (JVM) — commit status + PR comment, no CI shell

If you'd rather not add `curl`/`jq` steps to your CI, a JVM project can post from a Gradle task
itself, reading the same files. This posts a **commit status** (pass/fail + the `summary.txt`
headline) and, on a **pull request**, a single review **comment** carrying the full `summary.md` so
reviewers can triage inline. The comment is *upserted* via a hidden marker — repeated builds update
the one comment instead of spamming the PR. `GITHUB_TOKEN` only; every step self-skips when a
prerequisite is missing, and a failure only warns (the `buildchecks` gate decides pass/fail).

Nothing extra on the classpath — it uses `java.net.http` (JDK 11+, and unlike `HttpURLConnection`
it supports `PATCH`) and Groovy's `JsonSlurper`/`JsonOutput`, which ship with Gradle.

```kotlin
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import org.gradle.api.logging.Logger

/** Reads summary.json/.txt/.md from [outputDir] and publishes them to GitHub. */
class GitHubPublisher(private val outputDir: File, private val logger: Logger) {
    private val token = System.getenv("GITHUB_TOKEN").orEmpty()
    private val repo = System.getenv("GITHUB_REPOSITORY") ?: "OWNER/REPO"   // e.g. "acme/app"

    fun publish() {
        if (token.isBlank()) return logger.lifecycle("BuildChecks: no GITHUB_TOKEN; skipping.")
        commitStatus(); prComment()
    }

    private fun commitStatus() {
        val summary = File(outputDir, "summary.json").takeIf { it.isFile } ?: return
        val sha = env("BITRISE_GIT_COMMIT", "GITHUB_SHA") ?: gitHead() ?: return
        val passed = (JsonSlurper().parse(summary) as Map<*, *>)["passed"] == true
        report("commit status", send("POST", "statuses/$sha", mapOf(
            "state" to if (passed) "success" else "failure",
            "context" to "buildchecks",
            "description" to File(outputDir, "summary.txt").readText().trim().take(140),
        )))
    }

    private fun prComment() {
        val pr = env("BITRISE_PULL_REQUEST")                                 // add your CI's PR var
            ?: System.getenv("GITHUB_REF")?.let { Regex("refs/pull/(\\d+)/").find(it)?.groupValues?.get(1) }
        if (pr?.toIntOrNull() == null) return
        val body = "<!-- buildchecks -->\n" + File(outputDir, "summary.md").readText().trim()
        val id = findOurComment(pr)                                          // upsert: one comment, always current
        report("PR #$pr comment",
            if (id != null) send("PATCH", "issues/comments/$id", mapOf("body" to body))
            else            send("POST",  "issues/$pr/comments",  mapOf("body" to body)))
    }

    private fun findOurComment(pr: String): String? {
        val (code, body) = send("GET", "issues/$pr/comments?per_page=100", null)
        if (code !in 200..299) return null
        @Suppress("UNCHECKED_CAST")
        val comments = runCatching { JsonSlurper().parseText(body) as? List<Map<String, Any?>> }.getOrNull() ?: return null
        return comments.firstOrNull { (it["body"] as? String)?.contains("<!-- buildchecks -->") == true }?.get("id")?.toString()
    }

    private fun send(method: String, path: String, json: Map<String, Any?>?): Pair<Int, String> {
        val builder = HttpRequest.newBuilder(URI("https://api.github.com/repos/$repo/$path"))
            .header("Authorization", "Bearer $token").header("Accept", "application/vnd.github+json")
        val publisher = json?.let {
            builder.header("Content-Type", "application/json")
            HttpRequest.BodyPublishers.ofString(JsonOutput.toJson(it))
        } ?: HttpRequest.BodyPublishers.noBody()
        return runCatching { HttpClient.newHttpClient().send(builder.method(method, publisher).build(), HttpResponse.BodyHandlers.ofString()) }
            .fold({ it.statusCode() to it.body() }, { -1 to (it.message ?: "error") })
    }

    private fun report(what: String, r: Pair<Int, String>) =
        if (r.first in 200..299) logger.lifecycle("BuildChecks: posted $what.")
        else logger.warn("BuildChecks: $what failed (${r.first}). ${r.second.take(200)}")

    private fun env(vararg names: String) = names.firstNotNullOfOrNull { System.getenv(it)?.ifBlank { null } }
    private fun gitHead() = runCatching { ProcessBuilder("git", "rev-parse", "HEAD").start()
        .inputStream.bufferedReader().readText().trim().ifBlank { null } }.getOrNull()
}
```

Wire it as a finalizer that runs after the gate, so a failed gate still posts a red status. Capture
the output dir as a `File` at configuration time — don't reference `Project` from the task action,
or the configuration cache can't serialize it:

```kotlin
tasks.register("postChecks") {
    mustRunAfter("buildchecks")
    val outputDir = File(rootProject.buildDir, "reports/buildchecks")
    doLast { GitHubPublisher(outputDir, logger).publish() }
}
```

## GitLab

Declare `codeclimate.json` as a Code Quality report; GitLab renders the findings natively in the
merge-request widget. The job's exit code still gates the pipeline.

```yaml
buildchecks:
  script:
    - java -jar tools/buildchecks-4.0.0-all.jar check
  artifacts:
    when: always
    reports:
      codequality: build/reports/buildchecks/codeclimate.json
```

## Bitbucket

Post a build status with `curl`, same pattern as the GitHub one:

```bash
STATE=$(jq -r 'if .passed then "SUCCESSFUL" else "FAILED" end' build/reports/buildchecks/summary.json)
curl -sf -X POST -u "$BB_USER:$BB_APP_PASSWORD" \
  "https://api.bitbucket.org/2.0/repositories/$WORKSPACE/$REPO/commit/$COMMIT_SHA/statuses/build" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --arg s "$STATE" '{key:"buildchecks", state:$s, name:"BuildChecks", url:"'"$BUILD_URL"'"}')"
```

## Architecture rules (any ecosystem)

Layering/dependency-direction rules — [Konsist](https://docs.konsist.lemonappdev.com/) or
ArchUnit (JVM), dependency-cruiser (JS), import-linter (Python) — run as tests and emit JUnit
XML, which BuildChecks already ingests. So **architecture gates today with no new code**: a
violated rule is a failed test, and the test-failure cap fails the build. (Headlining layering
drift as its own category, rather than as a generic test failure, is a planned 4.1 enhancement.)
