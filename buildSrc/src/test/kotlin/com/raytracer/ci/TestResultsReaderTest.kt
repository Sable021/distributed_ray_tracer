package com.raytracer.ci

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class TestResultsReaderTest {

    @Test
    fun parsesCountsFromSingleSuite() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <testsuite name="FooTest" tests="10" skipped="2" failures="1" errors="1"></testsuite>
        """.trimIndent()
        val t = TestResultsReader.parse(xml)
        assertEquals(10, t.tests)
        assertEquals(1, t.failures)
        assertEquals(1, t.errors)
        assertEquals(2, t.skipped)
        assertEquals(6, t.passed)
        assertEquals(10, t.total)
    }

    @Test
    fun sumsMultipleSuitesInOneDocument() {
        val xml = """
            <testsuites>
              <testsuite tests="5" skipped="0" failures="0" errors="0"></testsuite>
              <testsuite tests="3" skipped="1" failures="1" errors="0"></testsuite>
            </testsuites>
        """.trimIndent()
        val t = TestResultsReader.parse(xml)
        assertEquals(8, t.tests)
        assertEquals(1, t.failures)
        assertEquals(1, t.skipped)
        assertEquals(6, t.passed)
    }

    @Test
    fun missingAttributesDefaultToZero() {
        val t = TestResultsReader.parse("""<testsuite tests="4"></testsuite>""")
        assertEquals(4, t.tests)
        assertEquals(0, t.failures)
        assertEquals(0, t.errors)
        assertEquals(0, t.skipped)
        assertEquals(4, t.passed)
    }

    @Test
    fun allPassingSuiteHasFullPassRate() {
        val t = TestResultsReader.parse("""<testsuite tests="92" skipped="0" failures="0" errors="0"></testsuite>""")
        assertEquals(92, t.passed)
        assertEquals(92, t.total)
    }
}
