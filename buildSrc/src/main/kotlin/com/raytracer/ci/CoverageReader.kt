package com.raytracer.ci

import com.raytracer.ci.XmlSupport.childElements
import com.raytracer.ci.XmlSupport.intAttr
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

data class Coverage(
    val lineCovered: Int,
    val lineMissed: Int,
    val branchCovered: Int,
    val branchMissed: Int,
) {
    val lineRatio: Double get() = ratio(lineCovered, lineMissed)
    val branchRatio: Double get() = ratio(branchCovered, branchMissed)

    private fun ratio(covered: Int, missed: Int): Double =
        if (covered + missed == 0) 0.0 else covered.toDouble() / (covered + missed)

    companion object {
        val ZERO = Coverage(0, 0, 0, 0)
    }
}

object CoverageReader {

    fun parse(xml: String): Coverage {
        val report = XmlSupport.parse(xml).documentElement
        // Only the <report>'s direct <counter> children hold the whole-report totals;
        // package/class/method counters are nested deeper and must be ignored.
        val counters = report.childElements("counter").associateBy { it.getAttribute("type") }
        val line = counters["LINE"]
        val branch = counters["BRANCH"]
        return Coverage(
            lineCovered = line?.intAttr("covered") ?: 0,
            lineMissed = line?.intAttr("missed") ?: 0,
            branchCovered = branch?.intAttr("covered") ?: 0,
            branchMissed = branch?.intAttr("missed") ?: 0,
        )
    }

    fun readReport(path: Path): Coverage =
        if (Files.isRegularFile(path)) parse(path.readText()) else Coverage.ZERO
}
