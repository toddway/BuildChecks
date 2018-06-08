Gradle plugin to log build status details.  Output can go to the console or post directly to BitBucket and GitHub build status APIs.

## Installation
Add the following to your root build.gradle file:

    plugins {
        id "com.toddway.buildstatusplugin" version "1.0"
    }

    apply plugin: BuildStatusPlugin

    buildstatus {
        statusBaseUrl = "https://github.com" //tested with https://bitbucket.example.com and http://github.com
        statusAuthorization = "Bearer <your repo token>"
        buildUrl = System.getenv('BITRISE_BUILD_URL') ? System.getenv('BITRISE_BUILD_URL') : "http://localhost"
        lintReports = "$projectDir/app/build/reports/lint-results-prodRelease.xml" //comma seperated paths to your lint xml reports
        jacocoReports = "$projectDir/core/build/reports/jacoco/coverage/coverage.xml" //comma seperated apths to your jacoco xml reports
        detektReports = "$projectDir/app/build/reports/detekt-checkstyle.xml" //comma separated paths to detekt xml reports
    }

Now on any gradle task, you should see build status info printed to the console.


## Usage
To post the status to github or bitbucket, append `-Ppost` to your gradle task.  For example if you had a gradle task like:

    task ci {
        dependsOn 'app:lintProdRelease'
        dependsOn ':detektCheck'
        dependsOn 'core:coverage'
        dependsOn 'app:assembleRelease'
    }

You would run the following to postStats status details to BitBucket

    ./gradlew ci -Ppost

To make sure you're not using data from old reports, add clean to your gradle command:

    ./gradlew clean ci -Ppost

To log details on the HTTP requests add -i

    ./gradlew clean ci -Ppost -i



## Install Gradle
Detailed instruction are [here](https://docs.gradle.org/current/userguide/installation.html).
You should have Java 7 or newer installed.  You can

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