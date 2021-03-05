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
    override var commitHash: String = ""
        get() = field.takeIf { it.isNotBlank() } ?: "git rev-parse HEAD".run() ?: ""
    override var shortHash: String = ""
        get() = field.takeIf { it.isNotBlank() } ?: "git rev-parse --short HEAD".run() ?: ""
    override var commitDate: Long = 0L
        get() = field.takeIf { it != 0L } ?: "git show -s --format=%ct".run()?.toLong() ?: 0
    override var commitBranch: String = ""
        get() =  field.takeIf { it.isNotBlank() } ?: "git symbolic-ref -q --short HEAD".run() ?: ""
    override var isAllCommitted: Boolean
        get() = "git status -s".run()?.isEmpty() ?: false
        set(value) {}
}


