package com.raytracer.ci

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readText

object ProjectScanner {

    data class ScanResult(val inputs: ProjectInputs, val coveragePresent: Boolean)

    fun scan(projectDir: Path): ScanResult {
        val main = parseTree(projectDir.resolve("src/main/java"))
        val test = parseTree(projectDir.resolve("src/test/java"))
        val testTotals = TestResultsReader.readDirectory(projectDir.resolve("build/test-results/test"))
        val coveragePath = projectDir.resolve("build/reports/jacoco/test/jacocoTestReport.xml")
        val coverage = CoverageReader.readReport(coveragePath)
        val inputs = ProjectInputs(
            main = main,
            test = test,
            testTotals = testTotals,
            coverage = coverage,
            readmeText = readIfExists(projectDir.resolve("README.md")),
            buildScriptText = readIfExists(projectDir.resolve("build.gradle.kts")),
            wrapperJarPresent = Files.isRegularFile(projectDir.resolve("gradle/wrapper/gradle-wrapper.jar")),
        )
        return ScanResult(inputs, Files.isRegularFile(coveragePath))
    }

    private fun parseTree(root: Path): List<JavaSource> {
        if (!Files.isDirectory(root)) return emptyList()
        Files.walk(root).use { stream ->
            return stream
                .filter { Files.isRegularFile(it) && it.extension == "java" }
                .map { MetricsCollector.parse(it, it.readText()) }
                .sorted { a, b -> a.path.compareTo(b.path) }
                .toList()
        }
    }

    private fun readIfExists(path: Path): String =
        if (Files.isRegularFile(path)) path.readText() else ""
}
