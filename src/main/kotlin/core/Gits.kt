import core.entity.*
import core.run
import core.runCommand
import java.io.File

fun BuildConfig.pushArtifacts(messageQueue: MutableList<Message>) {
    if (isPushActivated) {
        if (artifactsBranch.isNotBlank())
            pushArtifacts(artifactsBranch, artifactsDir(), tempDir(), log)
        else
            messageQueue.add(ErrorMessage("To run 'pushArtifacts', your BuildChecks config must define an artifactsBranch"))
    }
}

fun pushArtifacts(branchName: String, sourceDir: File?, tempDir: File, log: Log?) {
    if (sourceDir == null) return
    log?.info(InfoMessage("Executing pushArtifacts...").toString())
    tempDir.deleteRecursively()
    gitShallowClone(tempDir, branchName, log)
    if (!tempDir.exists()) initOrphanBranch(tempDir, branchName, log)
    removeAll(tempDir, log)
    sourceDir.copyRecursively(tempDir)
    commitAll(tempDir, File(sourceDir, "stats.csv").readText(), log)
    "git push --set-upstream origin $branchName".runCommand(tempDir, log)
    println(InfoMessage("Pushed ${sourceDir.path} to $branchName branch of ${gitUrl()}").toString())
}

fun initOrphanBranch(tempDir : File, branchName : String, log: Log?) {
    tempDir.mkdir()
    gitShallowClone(tempDir, null, null)
    "git checkout --orphan $branchName".runCommand(tempDir, log)
}

fun removeAll(tempDir: File, log: Log?) {
    "git rm -rfq --ignore-unmatch .".runCommand(tempDir, log)
    "git clean -dfq".runCommand(tempDir, log)
}

fun commitAll(tempDir: File, commitMessage : String, log: Log?) {
    "git add .".runCommand(tempDir, log)
    "git commit -q -m \"$commitMessage\"".runCommand(tempDir, log)
}

fun deleteBranch(branchName: String) {
    "git branch -d $branchName".run()
    "git push origin --delete $branchName".run()
}

fun getCommitLog(tempDir : File, branchName : String, log : Log?): String? {
    tempDir.deleteRecursively()
    "git clone ${gitUrl()} ${tempDir.path} --branch $branchName".runCommand(File("."), log)
    return "git --no-pager log --pretty=tformat:%B".runCommand(tempDir, log)
}

fun gitUrl() = "git config --get remote.origin.url".run()

fun gitShallowClone(tempPath: File, branchName: String? = null, log: Log?): String? {
    val branch = branchName?.let { " --branch $branchName" } ?: ""
    return "git clone ${gitUrl()} ${tempPath.path}$branch --depth 1".runCommand(File("."), log)
}