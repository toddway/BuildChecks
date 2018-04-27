Gradle plugin to log build status details.  Output can go to the console or post directly to BitBucket and GitHub build status APIs.

## Installation
From the root of your project, add as a submodule in a directory called `buildSrc`

    git submodule add https://github.com/toddway/buildStatusPlugin.git buildSrc

Add the following to your root build.gradle file:

    apply plugin: BuildStatusPlugin

    buildstatus {
        statusBaseUrl = "https://github.com" //tested with https://bitbucket.example.com and http://github.com
        statusAuthorization = "Bearer <your repo token>"
        buildUrl = System.getenv('BITRISE_BUILD_URL') ? System.getenv('BITRISE_BUILD_URL') : "http://localhost"
        lintReports = "$projectDir/app/build/reports/lint-results-prodRelease.xml" //comma seperated paths to your lint xml reports
        jacocoReports = "$projectDir/core/build/reports/jacoco/coverage/coverage.xml" //comma seperated apths to your jacoco xml reports
    }

Now on any gradle task, you should see build status info printed to the console.


## Usage
To postStats the status to github or bitbucket, append `-PpostStatus` to your gradle task.  For example if you had a gradle task like:

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