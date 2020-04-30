package core.entity

data class Stats(val coveragePercent : Double,
                 val issueCount : Int,
                 val durationSeconds : Double,
                 val linesPlusBranchesCount : Int,
                 val commitDate : Long,
                 val commitHash : String,
                 val commitBranch : String
) {
    override fun toString(): String {
        return "$coveragePercent=coverage, $linesPlusBranchesCount=lines+branches, $issueCount=violations, $durationSeconds=duration, $commitDate=date, $commitBranch=branch, $commitHash=hash"
    }
}
