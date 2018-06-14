Gradle plugin to post summaries from code analyzers (Jacoco, Android Lint, Detekt, Checkstyle, Cobertura)
to VCS APIS ([GitHub](https://developer.github.com/v3/repos/statuses/) & [BitBucket](https://developer.atlassian.com/server/bitbucket/how-tos/updating-build-status-for-commits/))

## Installation
If you're not already using Gradle on your project,
you should [install it](https://docs.gradle.org/current/userguide/installation.html)
and [initialize it for your project](https://guides.gradle.org/creating-new-gradle-builds/).

Then add the following to the build.gradle file at the root of your project:

    plugins {
        id "com.toddway.buildchecks" version "1.4"
    }

    buildChecks {
        baseUrl = "https://api.github.com/repos/<owner>/<repo>" //tested with https://bitbucket.<your server> and https://api.github.com/repos/<owner>/<repo>
        authorization = "Bearer <your repo token>" //Generate this on the Github or Bitbucket website for your project
        buildUrl = System.getenv('BUILD_URL') ? System.getenv('BUILD_URL') : "http://localhost"
        androidLintReports = "$projectDir/app/build/reports/lint-results-prodRelease.xml" //comma seperated paths to your Android lint xml reports
        checkstyleReports = "$projectDir/app/build/reports/detekt-checkstyle.xml" //comma separated paths to Checkstyle xml reports
        jacocoReports = "$projectDir/core/build/reports/jacoco/coverage/coverage.xml" //comma seperated apths to your JaCoCo xml reports
        coberturaReports = "$projectDir/functions/coverage/cobertura-coverage.xml" //comma separated paths to Cobertura xml reports (also supported by Istanbul)
    }

The buildChecks block lets you configure the details of your build outputs and your source control system.  All examples above are optional.



## Usage
To print build checks only to the console, run the `printChecks` Gradle task

    ./gradlew printChecks

To postAll build checks to your remote source control system, run the `postChecks` Gradle task

    ./gradlew postChecks

You will most likely want to attach the `postChecks` task to some other command that builds your project.
If you're already using a Gradle task for this (e.g. `build`, `assemble`, `mySpecialBuildTask`),
you can make your task depend on `postChecks` in your build.gradle:

    mySpecialBuildTask.dependsOn(postChecks)

Now simply running your task activates `postChecks`.

    ./gradlew mySpecialBuildTask

If you're running a non-Gradle command (e.g. `npm deploy`, `myBuildScript.sh`, `fastlane`),
you can attach postChecks by letting Gradle execute your command.
Gradle has a built in task type for external commands.  Here's an example build.gradle implementation:

    task mySpecialBuildTask(type: Exec) {
        workingDir 'path/to/optional/subdirectory'
        commandLine 'npm', 'deploy'
    }

    mySpecialBuildTask.dependsOn(postChecks)


Then run

    ./gradlew mySpecialBuildTask

The exit value of command (0 or 1) will determine if success or failure is posted.



License
-------

    Copyright 2018-Present Todd Way

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.