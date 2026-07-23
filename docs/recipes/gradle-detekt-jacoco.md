# Gradle & Android (detekt / Lint + JaCoCo)

Findings from [detekt](https://detekt.dev/) (or Android Lint) as SARIF, coverage from JaCoCo XML,
tests as JUnit XML — all already produced by a standard Gradle build. BuildChecks resolves from its
[Pages Maven repo](https://toddway.github.io/BuildChecks) and gates without a plugin. Plain JVM and
Android use the identical wiring; the only Android-specific work is one extra task (AGP registers no
`jacocoTestReport`) and turning on Lint's SARIF — see [Android](#android) below.

## Build wiring

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm")
    jacoco
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
}

detekt {
    ignoreFailures = true          // let BuildChecks gate, not detekt
}
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        sarif.required = true      // build/reports/detekt/detekt.sarif  — parsed & gated
        html.required = true       // build/reports/detekt/detekt.html   — linked from the report
    }
}

tasks.test { finalizedBy(tasks.jacocoTestReport) }
tasks.jacocoTestReport {
    reports {
        xml.required = true        // build/reports/jacoco/test/…xml  — parsed & gated
        html.required = true       // build/reports/jacoco/test/html/ — linked from the report
    }
}

// Resolve BuildChecks from its Pages Maven repo and run it as the single gate.
repositories { maven { url = uri("https://toddway.github.io/BuildChecks") } }  // alongside your mavenCentral()
val buildchecks by configurations.creating
dependencies { buildchecks("com.toddway:buildchecks:4.0.0") }
tasks.register<JavaExec>("buildchecks") {
    classpath = buildchecks
    mainClass.set("buildchecks.cli.MainKt")   // JavaExec launches `java -cp … <mainClass>`; it never
                                               // reads the jar's Main-Class, so name the entry point here
    args("check")
}
tasks.named("check") { finalizedBy("buildchecks") }
```

## Run

```bash
./gradlew test detekt   # produce reports (neither fails the build)
./gradlew buildchecks   # or just `./gradlew check`, which finalizes with it
```

## Viewable report — emit each tool's HTML too

BuildChecks parses the machine formats (SARIF / XML) to gate, and it also copies each tool's **own
HTML report** into its output dir and links it from `index.html`, so a reader clicks from the gate
summary straight into detekt's finding or JaCoCo's line-by-line coverage. This drill-down is one of
the report's most useful features, so emit both formats from each tool:

- **detekt** — `html.required = true` writes `detekt.html` beside `detekt.sarif`; BuildChecks links
  the same-basename sibling.
- **JaCoCo** — `html.required = true` writes an `html/` dir beside the XML; BuildChecks links it.
- **Unit tests** — no config needed: Gradle already writes `build/reports/tests/…`, which BuildChecks
  links automatically from the JUnit XML.

Without the HTML reports the gate still works — you just lose the click-through to each tool's detail.

## Android

Android emits the same standard formats, so it gates with the wiring above plus three tweaks:

```kotlin
// module build.gradle.kts (app or library)
plugins {
    id("com.android.application")            // or com.android.library
    id("org.jetbrains.kotlin.android")
    jacoco
    // id("io.gitlab.arturbosch.detekt") version "1.23.6"   // optional extra findings
}

android {
    testOptions {
        unitTests.all { it.extensions.configure<JacocoTaskExtension> { isIncludeNoLocationClasses = true } }
    }
    lint {
        sarifReport = true       // build/reports/lint-results-debug.sarif — ingested like detekt
        htmlReport = true        // build/reports/lint-results-debug.html  — linked from the report
    }
}

// AGP registers no jacocoTestReport, so add one (emit both XML to gate and HTML to link).
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    reports { xml.required = true; html.required = true }
    classDirectories.setFrom(fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")))
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(fileTree(layout.buildDirectory) { include("**/testDebugUnitTest.exec") })
}

// The single gate — identical to the JVM block above.
tasks.register<JavaExec>("buildchecks") {
    dependsOn("testDebugUnitTest", "jacocoTestReport", "lintDebug")   // produce the reports first
    classpath = buildchecks
    mainClass.set("buildchecks.cli.MainKt")
    args("check")
}
tasks.named("check") { finalizedBy("buildchecks") }
```

`testDebugUnitTest` writes JUnit XML under `build/test-results/…`, the report task writes JaCoCo XML
(and HTML) under `build/reports/…`, and Lint writes its SARIF (and HTML) under `build/reports/…` — all
default discovery locations, so **no `paths` config is needed**.

## Notes

- **First run:** snapshot the baseline once with a sibling task (the same `JavaExec` block with
  `args("baseline")`), then commit `buildchecks-baseline.txt`.
- **Multi-module:** apply the coverage/lint config per module (a `buildSrc` convention plugin is the
  tidy way — it also means the `JavaExec`/`mainClass` wiring is written once, not per module), but
  register the `buildchecks` **JavaExec at the root** so it aggregates every module's reports — each
  module's `build/…` is discovered and grouped by origin automatically:

  ```kotlin
  // root build.gradle.kts
  repositories { maven { url = uri("https://toddway.github.io/BuildChecks") } }
  val buildchecks by configurations.creating
  dependencies { buildchecks("com.toddway:buildchecks:4.0.0") }
  tasks.register<JavaExec>("buildchecks") {
      dependsOn(subprojects.map { "${it.path}:test" })   // + jacoco/detekt (or lint) tasks
      classpath = buildchecks
      mainClass.set("buildchecks.cli.MainKt")
      args("check")
  }
  ```

- **One format per tool, for findings:** detekt (and Android Lint) can emit SARIF *and* Checkstyle
  XML; ingesting both double-counts findings. Emit SARIF for gating — the HTML report above is for
  humans and is never parsed, so it never double-counts.
- **Architecture rules:** [Konsist](https://docs.konsist.lemonappdev.com/) run as tests emit JUnit
  XML, so a layering violation gates today with no extra config.
