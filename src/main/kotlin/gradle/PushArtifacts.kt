package gradle

import Registry
import core.entity.InfoMessage
import core.entity.gitShallowClone
import core.entity.gitUrl
import core.run
import core.runCommand
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files

open class PushArtifacts : DefaultTask() {
    var registry : Registry? = null

    @TaskAction
    fun taskAction() {
        registry?.config?.apply {
            val tempDir = System.getProperty("java.io.tmpdir")
            pushArtifacts(artifactsBranch, artifactsDir(), File(tempDir, "buildChecks"))
            println(InfoMessage("Artifacts pushed to ${gitUrl()}"))
        }
    }
}

fun pushArtifacts(branchName: String, sourceDir: File?, tempDir: File) {
    if (sourceDir == null) return
    tempDir.deleteRecursively()
    gitShallowClone(tempDir, branchName)
    if (!tempDir.exists()) initOrphanBranch(tempDir, branchName)
    removeAll(tempDir)
    sourceDir.copyRecursively(tempDir)
    commitAll(tempDir, File(sourceDir, "buildChecks.csv").readText())
    "git push --set-upstream origin $branchName".runCommand(tempDir)
}

fun initOrphanBranch(tempDir : File, branchName : String) {
    tempDir.mkdir()
    gitShallowClone(tempDir, null)
    "git checkout --orphan $branchName".runCommand(tempDir)
}

fun removeAll(tempDir: File) {
    "git rm -rf .".runCommand(tempDir)
    "git clean -d -f".runCommand(tempDir)
}

fun commitAll(tempDir: File, commitMessage : String) {
    "git add .".runCommand(tempDir)
    "git commit -m \"$commitMessage\"".runCommand(tempDir)
}

fun deleteBranch(branchName: String) {
    "git branch -d $branchName".run()
    "git push origin --delete $branchName".run()
}