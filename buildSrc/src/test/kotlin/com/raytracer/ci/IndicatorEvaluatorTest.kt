package com.raytracer.ci

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.nio.file.Path

class IndicatorEvaluatorTest {

    private val eps = 1e-6

    private fun source(name: String, body: String): JavaSource =
        MetricsCollector.parse(Path.of("$name.java"), body)

    private fun inputs(): ProjectInputs {
        val main = listOf(
            source("Light", "package p;\npublic sealed interface Light permits A, B, C, D, E, F { double e(); }"),
            source("Noise", "package p;\npublic final class Noise { double n() { return Math.random(); } }"),
        )
        return ProjectInputs(
            main = main,
            test = emptyList(),
            testTotals = TestTotals(tests = 100, failures = 10, errors = 0, skipped = 0),
            coverage = Coverage(lineCovered = 70, lineMissed = 30, branchCovered = 30, branchMissed = 70),
            readmeText = "",
            buildScriptText = """tasks.register("verifyImage") { }""",
            wrapperJarPresent = true,
        )
    }

    private fun scoreOf(id: String): Double =
        IndicatorEvaluator.evaluate(inputs()).getValue(id).score

    @Test
    fun lineCoverageBelowTargetScoresProportionally() {
        // 0.70 / 1.00 target = 70
        assertEquals(70.0, scoreOf("5.5"), eps)
    }

    @Test
    fun branchCoverageBelowTargetScoresProportionally() {
        // 0.30 / 0.90 target = 33.33
        assertEquals(100.0 / 3.0, scoreOf("7.7"), eps)
    }

    @Test
    fun testPassRateReflectsFailures() {
        assertEquals(90.0, scoreOf("1.1"), eps)
    }

    @Test
    fun mathRandomInProductionScoresZeroOnSecurity() {
        assertEquals(0.0, scoreOf("6.2"), eps)
    }

    @Test
    fun sealedPermitsAtTargetScoreFull() {
        assertEquals(100.0, scoreOf("8.3"), eps)
    }

    @Test
    fun goldenGatePresenceIsBinaryFull() {
        assertEquals(100.0, scoreOf("5.4"), eps)
    }

    private fun inputsWithHotPathInterface(method: String): ProjectInputs =
        inputs().copy(
            main = listOf(source("Light", "package p;\npublic interface Light { $method }"))
        )

    @Test
    fun parameterlessArrayAccessorOnHotPathIsNotAnAllocation() {
        // double[] diffuseEmission() returns a cached array — no per-call allocation.
        val m = IndicatorEvaluator.evaluate(inputsWithHotPathInterface("double[] diffuseEmission();"))
        assertEquals(100.0, m.getValue("2.3").score, eps)
    }

    @Test
    fun arrayReturningComputeMethodOnHotPathIsPenalised() {
        // A compute method (takes inputs) that returns a fresh array violates the out-param contract.
        val m = IndicatorEvaluator.evaluate(inputsWithHotPathInterface("double[] scatter(double[] wi);"))
        assertTrue(m.getValue("2.3").score < 100.0, "expected penalty, got ${m.getValue("2.3").score}")
    }

    @Test
    fun everyRegistryIndicatorIsEvaluated() {
        val produced = IndicatorEvaluator.evaluate(inputs()).keys
        val expected = AttributeRegistry.allIndicators().map { it.id }.toSet()
        assertEquals(expected, produced)
    }
}
