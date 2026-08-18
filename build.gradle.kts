plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
    jacoco // core Gradle plugin; enables dogfooding: the CLI gates its own build
    `maven-publish` // core; publishes the thin jar + POM to the GitHub Pages Maven repo
    // Build-time only (not a CLI runtime dependency): dogfoods mutation testing so the repo can gate
    // its own mutation score (V4-PLAN.md 4.1) and regenerate the PIT golden fixture. Its `pitest`
    // task is opt-in — it is not wired into `test`/`check`, so ordinary builds stay fast.
    id("info.solidsoft.pitest") version "1.15.0"
}

group = "com.toddway"
version = "4.1.2"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

application {
    mainClass = "buildchecks.cli.MainKt"
}

// Complete dependency list — additions need written justification (see V4-PLAN.md §2).
dependencies {
    implementation(libs.serialization.json)
    implementation(libs.ktoml)
    implementation(libs.clikt)
    implementation(libs.kotlinx.html)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.jar {
    manifest { attributes["Main-Class"] = application.mainClass }
}

// Self-contained jar for GitHub Releases / JavaExec without dependency resolution.
val fatJar by tasks.registering(Jar::class) {
    archiveClassifier = "all"
    // Reproducible bytes so the sha256 computed by release.sh matches the jar CI publishes.
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest { attributes["Main-Class"] = application.mainClass }
    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/versions/**/module-info.class", "module-info.class")
    }
}

tasks.assemble { dependsOn(fatJar) }

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports { xml.required = true }
}

// Mutation testing (V4-PLAN.md 4.1). Run `./gradlew pitest` to produce build/reports/pitest/*/mutations.xml,
// which BuildChecks ingests like any other report. Scoped to a few branch-heavy classes so a dogfood run
// stays quick; widen targetClasses for a fuller local run. No mutationThreshold: BuildChecks does the
// gating (changed-line mutation), not PIT itself.
pitest {
    junit5PluginVersion = "1.2.1"
    targetClasses = setOf(
        "buildchecks.render.SummaryText",
        "buildchecks.gate.CoverageGate",
        "buildchecks.gate.ChangedLineCoverageGate",
    )
    // Without this the default targetTests mirrors targetClasses, so no test runs and every mutant
    // reports NO_COVERAGE. Let the whole suite run against the mutants.
    targetTests = setOf("buildchecks.*")
    outputFormats = setOf("XML", "HTML")
    timestampedReports = false
}

// --- Publishing (V4-PLAN.md §7, §11) ---
// The thin jar + POM + Gradle module metadata are published to a plain file-based Maven layout
// under build/maven-repo; the release workflow pushes that layout to the `gh-pages` branch, where
// GitHub Pages serves it as https://toddway.github.io/BuildChecks. JVM consumers resolve
// `com.toddway:buildchecks:<version>` from there (its transitives come from the POM, fetched from
// the consumer's own mavenCentral()). The fat jar (`assemble`) ships on GitHub Releases for
// `java -jar` use without resolution. No signing, no Sonatype — publishing needs only GITHUB_TOKEN.
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name = "BuildChecks"
                description = "A toolchain-agnostic CLI that aggregates code-analysis and " +
                    "test/coverage reports into one gated summary."
                url = "https://github.com/toddway/BuildChecks"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "toddway"
                        name = "Todd Way"
                        url = "https://github.com/toddway"
                    }
                }
                scm {
                    url = "https://github.com/toddway/BuildChecks"
                    connection = "scm:git:https://github.com/toddway/BuildChecks.git"
                    developerConnection = "scm:git:ssh://git@github.com/toddway/BuildChecks.git"
                }
            }
        }
    }
    // A local Maven layout the release workflow copies onto the `gh-pages` branch.
    repositories {
        maven {
            name = "pages"
            url = uri(layout.buildDirectory.dir("maven-repo"))
        }
    }
}
