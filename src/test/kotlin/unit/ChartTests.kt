package unit

import core.entity.attr
import core.entity.children
import core.entity.toDocument
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartUtils
import org.jfree.chart.JFreeChart
import org.jfree.chart.labels.XYItemLabelGenerator
import org.jfree.chart.plot.PlotOrientation
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.awt.Font
import java.awt.geom.Ellipse2D
import java.io.File

class ChartTests {

    @Test
    fun `sadlfk`() {
        val file = File("./src/test/testFiles/jdepend-bc.xml")
        val elements = file.toDocument().children(listOf("Package"))
        val triples = elements.mapNotNull { it.toAITriple() }

        ChartUtils.saveChartAsPNG(
                File("./build/reports/${file.nameWithoutExtension}.png"),
                triples.toJFreeChart(),
                800,
                600
        )
    }
}

fun List<Triple<String, Double, Double>>.toJFreeChart() : JFreeChart {
    val dataset = XYSeriesCollection()

    val s0 = XYSeries("Packages", false, true)
    forEach { s0.add(it.third, it.second) }
    dataset.addSeries(s0)

    val s1 = XYSeries("Main Sequence")
    s1.add(1f, 0f)
    s1.add(0, 1)
    dataset.addSeries(s1)

    val chart = ChartFactory.createXYLineChart(
            "",
            "Instability",
            "Abstractness",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false)

    val renderer = XYLineAndShapeRenderer()
    renderer.setSeriesShapesVisible(0, true)
    renderer.setSeriesLinesVisible(0, false)
    renderer.setSeriesShape(0, Ellipse2D.Double(-3.toDouble(), -3.toDouble(), 6.toDouble(), 6.toDouble()))
    //renderer.setSeriesPositiveItemLabelPosition(0, ItemLabelPosition(ItemLabelAnchor.OUTSIDE3, TextAnchor.TOP_CENTER, TextAnchor.TOP_CENTER, - Math.PI / 10))
    renderer.defaultItemLabelsVisible = true
    val font = Font("SansSerif", Font.PLAIN, 8)
    renderer.setSeriesItemLabelFont(0, font)
    renderer.setSeriesItemLabelGenerator(0, XYItemLabelGenerator { dataset, seriesIndex, itemIndex ->
        //println("${this[itemIndex]} ${dataset.getX(seriesIndex, itemIndex)}, ${dataset.getY(seriesIndex, itemIndex)}")
        this[itemIndex].first
    })
    (chart.plot as XYPlot).renderer = renderer

    chart.plot.outlinePaint = null
    chart.borderPaint = null
    chart.isBorderVisible = false
    chart.borderStroke = null
    return chart
}

fun Node.toAITriple() : Triple<String, Double, Double>? {
    if (this !is Element) return null
    val a = getElementsByTagName("A")
    val i = getElementsByTagName("I")
    return if (a.length > 0 && i.length > 0) {
        val av = a.item(0).firstChild.textContent.toDouble()
        val iv = i.item(0).firstChild.textContent.toDouble()
        val name = attr("name")?.let {it} ?: "unknown"
        Triple(name, av, iv)
    } else null
}
