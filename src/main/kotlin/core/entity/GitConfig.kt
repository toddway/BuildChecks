package core.entity

import core.run
import java.text.SimpleDateFormat
import java.util.*

interface GitConfig {
    var commitHash : String
    var shortHash : String
    var commitDate : Long
    var commitBranch : String
    var isAllCommitted : Boolean
    fun summary() = "$shortHash $commitBranch at ${SimpleDateFormat().format(Date(commitDate * 1000))}"
}

open class GitConfigDefault : GitConfig {
    override var commitHash: String
        get() = "git rev-parse HEAD".run() ?: ""
        set(value) {}
    override var shortHash: String
        get() = "git rev-parse --short HEAD".run() ?: ""
        set(value) {}
    override var commitDate: Long
        get() = "git show -s --format=%ct".run()?.toLong() ?: 0
        set(value) {}
    override var commitBranch: String
        get() =  "git symbolic-ref -q --short HEAD".run() ?: ""
        set(value) {}
    override var isAllCommitted: Boolean
        get() = "git status -s".run()?.isEmpty() ?: false
        set(value) {}
}
