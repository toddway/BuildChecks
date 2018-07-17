package core.entity

interface GitConfig {
    var commitHash : String
    var shortHash : String
    var commitDate : Long
    var commitBranch : String
    var isAllCommitted : Boolean
}

open class GitConfigDefault : GitConfig {
    override var commitHash: String = commitHash()
    override var shortHash: String = shortHash()
    override var commitDate: Long = commitDate()
    override var commitBranch: String = commitBranch()
    override var isAllCommitted: Boolean = isAllCommitted()
}

fun commitHash() = "git rev-parse HEAD".run() ?: ""
fun shortHash()  = "git rev-parse --short HEAD".run() ?: ""
fun commitDate() = "git show -s --format=%ct".run()?.toLong() ?: 0
fun commitBranch() = "git symbolic-ref -q --short HEAD".run() ?: ""
fun isAllCommitted() = "git status -s".run()?.isEmpty() ?: false
