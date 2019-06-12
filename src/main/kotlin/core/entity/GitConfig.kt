package core.entity

import core.run

data class GitConfig (
    val commitHash : String = commitHash(),
    val shortHash : String = shortHash(),
    val commitDate : Long = commitDate(),
    val commitBranch : String = commitBranch(),
    var isAllCommitted : Boolean = isAllowedToPost()
)

fun commitHash() = "git rev-parse HEAD".run() ?: ""
fun shortHash()  = "git rev-parse --short HEAD".run() ?: ""
fun commitDate() = "git show -s --format=%ct".run()?.toLong() ?: 0
fun commitBranch() = "git symbolic-ref -q --short HEAD".run() ?: ""
fun isAllowedToPost() = "git status -s".run()?.isEmpty() ?: false
