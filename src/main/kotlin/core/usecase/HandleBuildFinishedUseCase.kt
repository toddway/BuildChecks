package core.usecase

import core.*
import core.entity.*
import java.io.File

class HandleBuildFinishedUseCase(
        private val postStatusUseCase: PostStatusUseCase,
        private val postStatsUseCase: PostStatsUseCase,
        private val getSummaryUseCases: List<GetSummaryUseCase>,
        private val config: BuildConfig,
        private val messageQueue : MutableList<Message>
) {
    fun invoke() {
        if (config.isPluginActivated) {
            getSummaryUseCases.postStatuses(postStatusUseCase)
            getSummaryUseCases.toStats(config).let { postStatsUseCase.invoke(it) }
            messageQueue.distinct().forEach { println(it.toString()) }
            config.buildSummaryReports()
        }
    }
}

fun BuildConfig.buildSummaryReports() {
    val dirs = reportDirs()
    val artifactsDir = dirs.copyInto(artifactsDir())
    val files = artifactsDir.findReportFiles()
    val xmlPairs = files.filter { it.isXML() }.toNameAndPathPairs(artifactsDir)
    val htmlPairs = files.filter { it.isReadableFormat() }.toNameAndPathPairs(artifactsDir)
    val summaries = files.toSummaries(this)
    val chartHtml = if (artifactsBranch.isNotBlank()) historyChart(commitCsvsToHistoryMaps()) else ""

    File(artifactsDir, "index.html").apply {
        writeText(htmlReport(summaries.toMessages(), htmlPairs, xmlPairs, chartHtml))
        println(InfoMessage("Browse reports at " + absoluteFile.toURI()))
    }

    File(artifactsDir, "stats.csv").apply {
        writeText(summaries.toStats(this@buildSummaryReports).toString())
    }
}


fun BuildConfig.htmlReport(messages: List<Message?>, htmlPairs: List<Pair<String, File>>, xmlPairs: List<Pair<String, File>>, chartHtml : String) = """
<html>
<style>body {font-family: Helvetica, Arial, sans-serif;line-height:160%;padding:20px} a:link{text-decoration:none}</style>
<body>
<h1>Build Checks Summary Report</h1>
${messages.joinToString("\n") { "$it<br/>" }}
<ul>${htmlPairs.joinToString("\n") { "<li><a href=\"${it.second}\">${it.first}</a></li>" }}</ul>
$chartHtml
<p style="color:grey;font-size:11px">
Generated using ${git.summary()} 
from ${xmlPairs.map { "${it.second}" }.joinToString(", ")}
</p>
</body>
</html>
""".trimIndent()

fun historyChart(history : List<Map<String, String>>) = """
<script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.13.0/moment.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/chart.js@2.8.0"></script>
<canvas id="myChart" style="max-width:1000px"></canvas>
<script>
var timeFormat = 'MM/DD/YYYY HH:mm';
function unixDate(stamp) {
	return moment.unix(stamp).format(timeFormat);
}

var coverageData = [${history.jsArrayItemsFrom("coverage")}];
var linePlusBranchData = [${history.jsArrayItemsFrom("lines+branches")}];
var durationData = [${history.jsArrayItemsFrom("duration")}];
var violationData = [${history.jsArrayItemsFrom("violations")}];

window.chartColor = {
	red: 'rgb(255, 99, 132)',
	red2: 'rgb(255, 99, 132, 0.2)',
	orange: 'rgb(255, 159, 64)',
	orange2: 'rgb(255, 159, 64, 0.2)',
	yellow: 'rgb(255, 205, 86)',
	yellow2: 'rgb(255, 205, 86, 0.2)',
	green: 'rgb(75, 192, 192)',
	green2: 'rgb(75, 192, 192, 0.2)',
	blue: 'rgb(54, 162, 235)',
	blue2: 'rgb(54, 162, 235, 0.2)',
	purple: 'rgb(153, 102, 255)',
	purple2: 'rgb(153, 102, 255, 0.2)',
	grey: 'rgb(201, 203, 207)',
	grey2: 'rgb(201, 203, 207, 0.2)'
};

var config = {
    type: 'line',
    data: {
		datasets: [
		    {
                label: 'Lines + Branches',
                backgroundColor: window.chartColor.blue2,
                borderColor: window.chartColor.blue,
                borderWidth: 1,
                fill: true,
                yAxisID: 'y-axis-2',
                data: linePlusBranchData,
            },
            {
                label: 'Coverage %',
                backgroundColor: window.chartColor.red2,
                borderColor: window.chartColor.red,
                borderWidth: 1,
                fill: true,
                yAxisID: 'y-axis-1',
                data: coverageData,
            },
            {
                label: 'Violations',
                backgroundColor: window.chartColor.green2,
                borderColor: window.chartColor.green,
                borderWidth: 1,
                fill: true,
                yAxisID: 'y-axis-4',
                data: violationData,
                hidden: true,
            },
            {
                label: 'Duration',
                backgroundColor: window.chartColor.orange2,
                borderColor: window.chartColor.orange,
                borderWidth: 1,
                fill: true,
                yAxisID: 'y-axis-3',
                data: durationData,
                hidden: true,
            },
		]
	},
    options: {
        responsive: true,
        scales: {
            xAxes: [{
                type: 'time',
                display: true,
                time: {
                    parser: timeFormat,
                    tooltipFormat: 'll HH:mm'
                },
            }],
            yAxes: [
                {
                    display: true,
                    position: 'right',
                    id: 'y-axis-1',
                    ticks: {
                        fontColor: window.chartColor.red
                    }
                },
                {
                    display: true,
                    position: 'left',
                    id: 'y-axis-2',
                    ticks: {
                        fontColor: window.chartColor.blue
                    }
                },
                {
                    display: true,
                    position: 'right',
                    id: 'y-axis-3',
                    ticks: {
                        fontColor: window.chartColor.orange
                    }
                },
                {
                    display: true,
                    position: 'right',
                    id: 'y-axis-4',
                    ticks: {
                        fontColor: window.chartColor.green
                    }
                }
            ]
        }
    }
}

var chart = new Chart(document.getElementById('myChart').getContext('2d'), config);
</script>
""".trimIndent()

fun BuildConfig.commitCsvsToHistoryMaps() : List<Map<String, String>> {
    tempDir().deleteRecursively()
    "git clone ${gitUrl()} ${tempDir().path} --branch $artifactsBranch".run()
    val log = "git --no-pager log --pretty=tformat:%B".runCommand(tempDir())
    return log?.lines()?.filter { it.isNotBlank() }?.map { row ->
        val rowMap = mutableMapOf<String, String>()
        val cols = row.replace("\"", "").replace(" ","").split(",")
        cols.forEach { col ->
            col.split("=").let {
                if (it.size == 2) rowMap.put(it[1], it[0])
                else rowMap.put("unknown", it[0])
            }
        }
        rowMap
    }?.sortedBy { it["date"] } ?: listOf()
}

fun List<Map<String, String>>.jsArrayItemsFrom(key : String) : String {
    return joinToString(",") { "{ x:unixDate(\"${it["date"]}\"), y:${it[key]} }" }
}