# Integrating BuildChecks

BuildChecks is a CLI, not a build plugin. Every toolchain integrates the same way: run your
tests and analyzers with failures tolerated, then run `buildchecks check` and let its exit
code gate the build. The examples below assume the fat jar (`buildchecks-4.0.0-all.jar`) or
a `buildchecks` launcher on the PATH.

## Gradle

```kotlin
// root build.gradle.kts
repositories { maven { url = uri("https://toddway.github.io/BuildChecks") } }  // the BuildChecks Maven repo

val buildchecks by configurations.creating
dependencies { buildchecks("com.toddway:buildchecks:4.0.0") }

tasks.register<JavaExec>("buildchecks") {
    classpath = buildchecks
    mainClass.set("buildchecks.cli.MainKt")   // JavaExec is a classpath launch (java -cp … <mainClass>);
                                               // it never reads the jar's Main-Class, so name it here
    args("check")
}
tasks.named("check") { finalizedBy("buildchecks") }
```

Keep your usual `mavenCentral()` (or `google()`) repository alongside the line above — Gradle
resolves the BuildChecks jar from the Pages repo and its four runtime dependencies from Central.
Nothing is committed to your repo; Gradle caches the jar like any other dependency.

For Android specifically (per-variant JaCoCo report task, Lint SARIF), see the Android section of
[docs/recipes/gradle-detekt-jacoco.md](recipes/gradle-detekt-jacoco.md#android).

Why this is cache-safe (what v3 used a `BuildEventService` for):

- The `buildchecks` task declares no outputs, so Gradle never marks it `UP-TO-DATE`; it runs
  on every invocation.
- Upstream `UP-TO-DATE`/`FROM-CACHE` tasks are harmless by Gradle's contract: *skipped* means
  the report files on disk already match current sources; *restored from cache* means they
  were just materialized. Either way the files read are correct for the current code.
- The real residual risk is orphaned report files from removed modules — `check` emits a
  freshness warning when ingested reports differ in age beyond the configured tolerance.

## Maven

```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>exec-maven-plugin</artifactId>
  <executions>
    <execution>
      <id>buildchecks</id>
      <phase>verify</phase>
      <goals><goal>exec</goal></goals>
      <configuration>
        <executable>java</executable>
        <arguments>
          <argument>-jar</argument>
          <argument>${project.basedir}/tools/buildchecks-4.0.0-all.jar</argument>
          <argument>check</argument>
        </arguments>
      </configuration>
    </execution>
  </executions>
</plugin>
```

Run analyzers and tests in earlier phases with failures tolerated (e.g. surefire
`testFailureIgnore=true`); `buildchecks check` in `verify` is the single gate.

## npm

```json
{
  "scripts": {
    "test": "jest --coverage || true",
    "lint": "eslint --format @microsoft/eslint-formatter-sarif --output-file build/reports/eslint.sarif . || true",
    "check": "npm run test && npm run lint && java -jar tools/buildchecks-4.0.0-all.jar check"
  }
}
```

The `|| true` keeps tool failures from short-circuiting the pipeline before BuildChecks can
aggregate and gate them.

## Make

```make
.PHONY: check
check:
	-swiftlint lint --reporter sarif > build/reports/swiftlint.sarif
	-xcodebuild test ... # or your test runner, emitting JUnit XML + coverage
	java -jar tools/buildchecks-4.0.0-all.jar check
```

The leading `-` on tool lines tells Make to continue past their exit codes; the final
`buildchecks check` line is the one that fails the target.
