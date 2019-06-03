A Gradle plugin to post summaries from code analyzers to [GitHub](https://developer.github.com/v3/repos/statuses/) & [BitBucket](https://developer.atlassian.com/server/bitbucket/how-tos/updating-build-status-for-commits/).  E.g.:

[ ![Gradle Plugin](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/toddway/buildchecks/com.toddway.buildchecks.gradle.plugin/maven-metadata.xml.svg?label=Gradle%20Plugin) ](https://bintray.com/toddway/maven/shelf/_latestVersion)


    BUILD SUCCESSFUL in 11s
    9 actionable tasks: 7 executed, 2 up-to-date
    ✔ 10.05s for gradle postChecks
    ✔ 79.33% coverage, threshold is 70.0%
    ✔ 5 rule violations (5 warning), threshold is 5
    ✔ Posting to GITHUB


The plugin parses common output formats (Cobertura, JaCoCo, Checkstyle, Android Lint)
supported by many lint and coverage tools (Detekt, SwiftLint, ESLint, TSLint, Istanbul)

## Installation
If you're not already using Gradle on your project,
you should [install it](https://docs.gradle.org/current/userguide/installation.html)
and [initialize it for your project](https://guides.gradle.org/creating-new-gradle-builds/).

Then add the following to the build.gradle file at the root of your project:

    plugins {
        id "com.toddway.buildchecks" version "$buildchecks_version"
    }

## Usage
To print build checks only to the console, run the `printChecks` Gradle task

    ./gradlew printChecks

To post build checks to your remote source control system, run the `postChecks` Gradle task

    ./gradlew postChecks

You will most likely want to attach the `postChecks` task to some other command that builds your project and runs your lint and coverage tools.
If you're already using a Gradle task for this (e.g. `build`, `assemble`, `myCustomTask`),
you can make postChecks depend on your task. In your build.gradle:

    postChecks.dependsOn(myCustomTask)

Then calling `postChecks` will automatically activate `myCustomTask`:

    ./gradlew postChecks

If you're running a non-Gradle command (e.g. `npm deploy`, `myBuildScript.sh`, `fastlane`),
you can attach postChecks by letting Gradle execute your command.
Gradle lets you define custom executables like this:

    task myCustomTask(type: Exec) {
        workingDir 'path/to/optional/subdirectory'
        commandLine 'npm', 'deploy'
    }

    postChecks.dependsOn(myCustomTask)


The exit value of the external command (0 or 1) will determine if success or failure is posted for the build.

## Config
To configure the details of your build output and your source control system, add a buildChecks block to your build.gradle.
All example properties below are optional.

    buildChecks {
        baseUrl = "https://api.github.com/repos/<owner>/<repo>" 
        authorization = "Basic <your generated token>"
        buildUrl = System.getenv('BUILD_URL') ? System.getenv('BUILD_URL') : "http://localhost"
        androidLintReports = "$projectDir/build/reports/lint-results-prodRelease.xml" //comma seperated paths to your Android lint xml reports
        checkstyleReports = "$projectDir/build/reports/detekt-checkstyle.xml" //comma separated paths to Checkstyle xml reports
        jacocoReports = "$projectDir/build/reports/jacoco/coverage/coverage.xml" //comma seperated apths to your JaCoCo xml reports
        coberturaReports = "$projectDir/functions/coverage/cobertura-coverage.xml" //comma separated paths to Cobertura xml reports (also supported by Istanbul)
        minCoveragePercent = 80 
        maxLintViolations = 5 
     }

#### baseUrl
Github - `https://api.github.com/repos/<owner>/<repo>`

Bitbucket (REST 2.0) - `https://api.bitbucket.org/2.0/repositories/<owner>/<repo>/`

Bitbucket Server (REST 1.0) - `https://bitbucket.yourserver.com`


#### authorization
Generate basic authorization for Github (or Bitbucket) with this command:

    curl -v -u username:password github.com
    
Then find the following line and use the result in your BuildChecks config. 

    > Authorization: Basic <your generated token>

Instead of using your actual account password in the curl command, Github and Bitbucket both offer safer substitutes.  
 
Github - Your profile image -> Settings -> Developer Settings -> Personal Access Tokens.

Bitbucket - Your profile image -> Bitbucket settings -> App passwords. 

Bitbucket Server - Your profile image -> Manage account -> Personal access tokens.



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
    