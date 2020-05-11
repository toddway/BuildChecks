package core

import core.entity.BuildConfig
import core.entity.InfoMessage
import core.entity.Message
import core.entity.summary
import getCommitLog
import java.io.File


fun BuildConfig.htmlReport(messages: List<Message?>, htmlPairs: List<Pair<String, File>>, xmlPairs: List<Pair<String, File>>, chartHtml : String) = """
<html>
<style>body {font-family: Helvetica, Arial, sans-serif;line-height:160%;padding:20px} a:link{text-decoration:none}</style>
<body>
<h1>Build Summary</h1>
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
                hidden: false,
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

fun csvToMapsList(csv : String?) : List<Map<String, String>> {
    return csv?.lines()?.filter { it.isNotBlank() }?.map { row ->
        val rowMap = mutableMapOf<String, String>()
        val cols = row.replace("\"", "").replace(", ",",").split(",")
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
    return filter { it["date"] != null && it[key] != null }
            .joinToString(",") { "{ x:unixDate(\"${it["date"]}\"), y:${it[key]} }" }
}

fun BuildConfig.chartHtml(stats: String): String {
    return if (artifactsBranch.isNotBlank() && isPushActivated) {
        log?.info(InfoMessage("Building history chart from '$artifactsBranch' branch log...").toString())
        val commits = getCommitLog(tempDir(), artifactsBranch, log)
        log?.info(InfoMessage("Extract stat history from csv commit messages...").toString())
        val mapsList = csvToMapsList(commits + "\n" + stats)
        mapsList.forEach { log?.info(InfoMessage("$it").toString()) }
        historyChart(mapsList)
    }
    else ""
}