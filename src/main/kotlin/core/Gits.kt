package core

fun commitHash() = "git rev-parse HEAD".run() ?: ""
fun shortHash()  = "git rev-parse --short HEAD".run() ?: ""
fun commitDate() = "git show -s --format=%ct".run()?.toLong() ?: 0
fun commitBranch() = "git symbolic-ref -q --short HEAD".run() ?: ""
fun isAllCommitted() = "git status -s".run()?.isEmpty() ?: false
