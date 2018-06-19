package core.entity

import core.run

interface GitConfig {
    var commitHash : String
    var shortHash : String
    var commitDate : Long
    var commitBranch : String
    var isAllCommitted : Boolean
}

open class GitConfigDefault : GitConfig {
    override var commitHash: String = core.entity.commitHash()
    override var shortHash: String = core.entity.shortHash()
    override var commitDate: Long = core.entity.commitDate()
    override var commitBranch: String = core.entity.commitBranch()
    override var isAllCommitted: Boolean = core.entity.isAllCommitted()
}

fun commitHash() = "git rev-parse HEAD".run() ?: ""
fun shortHash()  = "git rev-parse --short HEAD".run() ?: ""
fun commitDate() = "git show -s --format=%ct".run()?.toLong() ?: 0
fun commitBranch() = "git symbolic-ref -q --short HEAD".run() ?: ""
fun isAllCommitted() = "git status -s".run()?.isEmpty() ?: false
