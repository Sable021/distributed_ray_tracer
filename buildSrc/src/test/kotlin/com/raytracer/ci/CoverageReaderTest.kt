package com.raytracer.ci

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class CoverageReaderTest {

    private val eps = 1e-9

    private val report = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE report PUBLIC "-//JACOCO//DTD Report 1.1//EN" "report.dtd">
        <report name="ray_tracer">
          <package name="com/raytracer/geom">
            <class name="com/raytracer/geom/Sphere">
              <counter type="LINE" missed="1" covered="9"/>
              <counter type="BRANCH" missed="2" covered="2"/>
            </class>
            <counter type="LINE" missed="1" covered="9"/>
            <counter type="BRANCH" missed="2" covered="2"/>
          </package>
          <counter type="INSTRUCTION" missed="100" covered="300"/>
          <counter type="BRANCH" missed="10" covered="30"/>
          <counter type="LINE" missed="20" covered="80"/>
          <counter type="COMPLEXITY" missed="5" covered="15"/>
          <counter type="METHOD" missed="2" covered="8"/>
          <counter type="CLASS" missed="0" covered="3"/>
        </report>
    """.trimIndent()

    @Test
    fun readsReportLevelLineCounter() {
        val c = CoverageReader.parse(report)
        assertEquals(80, c.lineCovered)
        assertEquals(20, c.lineMissed)
        assertEquals(0.8, c.lineRatio, eps)
    }

    @Test
    fun readsReportLevelBranchCounter() {
        val c = CoverageReader.parse(report)
        assertEquals(30, c.branchCovered)
        assertEquals(10, c.branchMissed)
        assertEquals(0.75, c.branchRatio, eps)
    }

    @Test
    fun ignoresNestedPackageAndClassCounters() {
        // Report-level LINE is 20/80, not the nested 1/9 — proves we read only root children.
        val c = CoverageReader.parse(report)
        assertEquals(100, c.lineCovered + c.lineMissed)
    }

    @Test
    fun absentCounterYieldsZeroRatioNotDivideByZero() {
        val empty = """
            <report name="empty">
              <counter type="INSTRUCTION" missed="0" covered="0"/>
            </report>
        """.trimIndent()
        val c = CoverageReader.parse(empty)
        assertEquals(0.0, c.lineRatio, eps)
        assertEquals(0.0, c.branchRatio, eps)
    }
}
