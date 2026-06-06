package com.raytracer.ci

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ReportWriterTest {

    private fun report(): ChangeabilityReport {
        val measures = AttributeRegistry.allIndicators().associate { it.id to Measure(0.0, 50.0) }
        return Aggregator.aggregate(measures, "2026-06-06T00:00:00Z")
    }

    @Test
    fun jsonCarriesIndexAndTimestamp() {
        val json = ReportWriter.toJson(report())
        assertTrue(json.contains("\"computedAt\": \"2026-06-06T00:00:00Z\""), json)
        assertTrue(json.contains("\"index\": 50.0"), json)
    }

    @Test
    fun jsonContainsEveryAttributeAndIndicatorId() {
        val json = ReportWriter.toJson(report())
        AttributeRegistry.attributes.forEach { attr ->
            assertTrue(json.contains("\"${attr.id}\""), "missing attribute ${attr.id}")
        }
        AttributeRegistry.allIndicators().forEach { ind ->
            assertTrue(json.contains("\"${ind.id}\""), "missing indicator ${ind.id}")
        }
    }

    @Test
    fun jsonIsLocaleStableForDecimals() {
        // Must use '.' as the decimal separator regardless of the default locale.
        val json = ReportWriter.toJson(report())
        assertFalse(json.contains("50,0"), "decimal rendered with a comma separator")
    }

    @Test
    fun markdownHasTitleAndAllNineAttributeRows() {
        val md = ReportWriter.toMarkdown(report())
        assertTrue(md.contains("Changeability Index"), md)
        AttributeRegistry.attributes.forEach { attr ->
            assertTrue(md.contains("| ${attr.displayName} |"), "missing row for ${attr.displayName}")
        }
    }

    @Test
    fun consoleTableSummarisesGrandTotal() {
        val text = ReportWriter.toConsole(report())
        assertTrue(text.contains("50.0"), text)
        assertTrue(text.contains("Changeability Index"), text)
    }
}
