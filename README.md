Gradle plugin to log build status details.  Output can go to the console or post directly to BitBucket and GitHub build status APIs.

## Installation
From the root of your project, add as a submodule in a directory called `buildSrc`

    git submodule add ssh://git@bitbucket-ssh.uhub.biz:7999/vmlnavmlmob/buildsrc.git buildSrc

Add the following to your root build.gradle file:

    apply plugin: BuildStatusPlugin

    buildstatus {
        postBaseUrl = "https://bitbucket.uhub.biz" //tested with https://bitbucket.uhub.biz and http://github.com
        postAuthorization = "Bearer <your repo token>"
        buildUrl = System.getenv('BITRISE_BUILD_URL') ? System.getenv('BITRISE_BUILD_URL') : "http://localhost"
        lintReports = "$projectDir/app/build/reports/lint-results-prodRelease.xml" //comma seperated paths to your lint xml reports
        jacocoReports = "$projectDir/core/build/reports/jacoco/coverage/coverage.xml" //comma seperated apths to your jacoco xml reports
    }

Now on any gradle task, you should see build status info printed to the console.


## Usage
To post the status to github or bitbucket, append `-PpostStatus` to your gradle task.  For example if you had a gradle task like:

    task ci {
        dependsOn 'app:lintProdRelease'
        dependsOn ':detektCheck'
        dependsOn 'core:coverage'
        dependsOn 'app:assembleRelease'
    }

You would run the following to post status details to BitBucket

    ./gradlew ci -PpostStatus

To make sure you're not using data from old reports, add clean to your gradle command:

    ./gradlew clean ci -PpostStatus

To log details on the HTTP POST commands add -i

    ./gradlew clean ci -PpostStatus -i

## TODO
- github datasource
- number of tests?
- lines of code?
- detekt
- checkstyle
- artifacts
