package com.raytracer.ci

import com.raytracer.ci.XmlSupport.elementsByTag
import com.raytracer.ci.XmlSupport.intAttr
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readText

data class TestTotals(
    val tests: Int,
    val failures: Int,
    val errors: Int,
    val skipped: Int,
) {
    val passed: Int get() = tests - failures - errors - skipped
    val total: Int get() = tests

    operator fun plus(other: TestTotals) = TestTotals(
        tests + other.tests,
        failures + other.failures,
        errors + other.errors,
        skipped + other.skipped,
    )

    companion object {
        val ZERO = TestTotals(0, 0, 0, 0)
    }
}

object TestResultsReader {

    fun parse(xml: String): TestTotals {
        val doc = XmlSupport.parse(xml)
        return doc.documentElement.elementsByTag("testsuite")
            .ifEmpty {
                // Root itself is a single <testsuite>.
                if (doc.documentElement.nodeName == "testsuite") listOf(doc.documentElement) else emptyList()
            }
            .fold(TestTotals.ZERO) { acc, suite ->
                acc + TestTotals(
                    suite.intAttr("tests"),
                    suite.intAttr("failures"),
                    suite.intAttr("errors"),
                    suite.intAttr("skipped"),
                )
            }
    }

    fun readDirectory(dir: Path): TestTotals {
        if (!Files.isDirectory(dir)) return TestTotals.ZERO
        Files.walk(dir).use { stream ->
            return stream
                .filter { Files.isRegularFile(it) && it.extension == "xml" }
                .map { parse(it.readText()) }
                .reduce(TestTotals.ZERO) { a, b -> a + b }
        }
    }
}
