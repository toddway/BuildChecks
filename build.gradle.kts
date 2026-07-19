plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
    jacoco // core Gradle plugin; enables dogfooding: the CLI gates its own build
}

group = "com.toddway"
version = "4.0.0-SNAPSHOT"

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

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

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
