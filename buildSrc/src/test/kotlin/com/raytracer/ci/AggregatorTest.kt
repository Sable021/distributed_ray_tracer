package com.raytracer.ci

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class AggregatorTest {

    private val eps = 1e-9

    private fun measures(score: Double): Map<String, Measure> =
        AttributeRegistry.allIndicators().associate { it.id to Measure(0.0, score) }

    @Test
    fun allIndicatorsPerfectYieldsOneHundred() {
        val report = Aggregator.aggregate(measures(100.0))
        assertEquals(100.0, report.index, eps)
    }

    @Test
    fun allIndicatorsZeroClampsToOneNotZero() {
        val report = Aggregator.aggregate(emptyMap())
        assertEquals(1.0, report.index, eps)
    }

    @Test
    fun uniformFiftyPropagatesThroughEveryWeighting() {
        val report = Aggregator.aggregate(measures(50.0))
        assertEquals(50.0, report.index, eps)
        report.attributes.forEach { assertEquals(50.0, it.score, eps) }
    }

    @Test
    fun attributeScoreIsWithinWeightWeightedAverage() {
        // Functional Suitability indicators: 1.1(w3)=100, 1.2(w3)=0, 1.3(w2)=100, 1.4(w2)=0
        // weighted = (100*3 + 0*3 + 100*2 + 0*2) / (3+3+2+2) = 500/10 = 50
        val m = mapOf(
            "1.1" to Measure(0.0, 100.0),
            "1.2" to Measure(0.0, 0.0),
            "1.3" to Measure(0.0, 100.0),
            "1.4" to Measure(0.0, 0.0),
        )
        val report = Aggregator.aggregate(m)
        val fs = report.attributes.first { it.id == "functionalSuitability" }
        assertEquals(50.0, fs.score, eps)
    }

    @Test
    fun missingMeasureScoresZeroForThatIndicator() {
        val report = Aggregator.aggregate(mapOf("7.1" to Measure(1.0, 85.0)))
        val maint = report.attributes.first { it.id == "maintainability" }
        val ind = maint.indicators.first { it.id == "7.1" }
        assertEquals(85.0, ind.score, eps)
        assertEquals(0.0, maint.indicators.first { it.id == "7.2" }.score, eps)
    }

    @Test
    fun everyRegistryIndicatorAppearsInReport() {
        val report = Aggregator.aggregate(emptyMap())
        val reported = report.attributes.flatMap { it.indicators }.map { it.id }.toSet()
        assertEquals(AttributeRegistry.allIndicators().map { it.id }.toSet(), reported)
    }
}
