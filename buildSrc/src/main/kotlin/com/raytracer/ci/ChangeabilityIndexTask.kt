package com.raytracer.ci

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.time.Instant

abstract class ChangeabilityIndexTask : DefaultTask() {

    @get:Internal
    abstract val projectRoot: DirectoryProperty

    @get:Internal
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val enforceFloor: Property<Boolean>

    @get:Input
    abstract val floor: Property<Double>

    @TaskAction
    fun compute() {
        val root = projectRoot.get().asFile.toPath()
        val scan = ProjectScanner.scan(root)
        if (!scan.coveragePresent) {
            logger.warn(
                "[changeabilityIndex] No JaCoCo report at build/reports/jacoco/test/jacocoTestReport.xml; " +
                    "coverage indicators (5.5, 7.7) score 0. Run `./gradlew test jacocoTestReport changeabilityIndex`."
            )
        }

        val measures = IndicatorEvaluator.evaluate(scan.inputs)
        val report = Aggregator.aggregate(measures, Instant.now().toString())

        val out = outputDir.get().asFile
        out.mkdirs()
        out.resolve("index.json").writeText(ReportWriter.toJson(report))
        out.resolve("report.md").writeText(ReportWriter.toMarkdown(report))

        logger.lifecycle("\n" + ReportWriter.toConsole(report))
        logger.lifecycle("\nFull report: ${out.resolve("report.md")}")

        if (enforceFloor.get() && report.index < floor.get()) {
            throw GradleException(
                "Changeability Index ${"%.1f".format(report.index)} is below the floor ${floor.get()}."
            )
        }
    }
}
