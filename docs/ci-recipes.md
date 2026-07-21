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
