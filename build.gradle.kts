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
