plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
    jacoco // core Gradle plugin; enables dogfooding: the CLI gates its own build
    `maven-publish` // core; publishes the thin jar + POM to Maven Central
    signing // core; PGP signatures Maven Central requires
}

group = "com.toddway"
version = "4.0.0"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
    withSourcesJar() // Maven Central requires -sources and -javadoc jars alongside the artifact
    withJavadocJar()
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

// --- Publishing (V4-PLAN.md §7, §11) ---
// Maven Central gets the thin jar + POM so the Gradle JavaExec snippet can resolve it and its
// deps; the fat jar (`assemble`) ships on GitHub Releases for `java -jar` use without resolution.
// Only core plugins are used, and signing is required only when a key is present, so a plain
// `./gradlew build` on a contributor machine never needs credentials.
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
    // A local Maven layout to zip and upload to the Central Portal (see MAINTAINING.md).
    repositories {
        maven {
            name = "staging"
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

signing {
    // Two supported credential shapes, whichever you have on hand:
    //  - in-memory: SIGNING_KEY (ASCII-armored private key) + SIGNING_PASSWORD env — CI-friendly,
    //    and avoids depending on a secretKeyRingFile path;
    //  - classic: signing.keyId / signing.password / signing.secretKeyRingFile gradle properties
    //    (put them in the gitignored repo-root gradle.properties).
    // With neither present, signing is skipped so a plain `./gradlew build` needs no key.
    val inMemoryKey = providers.environmentVariable("SIGNING_KEY").orNull
    val hasKeyringProps = providers.gradleProperty("signing.keyId").isPresent
    isRequired = inMemoryKey != null || hasKeyringProps
    if (inMemoryKey != null) {
        useInMemoryPgpKeys(inMemoryKey, providers.environmentVariable("SIGNING_PASSWORD").orNull)
    }
    sign(publishing.publications["maven"])
}
