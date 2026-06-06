package com.raytracer.ci

import com.raytracer.ci.ScoringFunctions.binary
import com.raytracer.ci.ScoringFunctions.passRate
import com.raytracer.ci.ScoringFunctions.penaltyPerCount
import com.raytracer.ci.ScoringFunctions.piecewiseLinear
import com.raytracer.ci.ScoringFunctions.ratioToTarget
import com.raytracer.ci.ScoringFunctions.invertedDistance

data class ProjectInputs(
    val main: List<JavaSource>,
    val test: List<JavaSource>,
    val testTotals: TestTotals,
    val coverage: Coverage,
    val readmeText: String,
    val buildScriptText: String,
    val wrapperJarPresent: Boolean,
)

object IndicatorEvaluator {

    // Interfaces whose methods must use caller-owned double[] out-params, never allocate a return.
    private val HOT_PATH_INTERFACES = setOf(
        "Primitive", "BRDF", "Light", "Texture", "Accelerator",
        "RenderStrategy", "PathIntegrator", "Sampler", "RandomSource",
    )
    private val LIBRARY_PACKAGE_SUFFIXES = listOf("geom", "shading", "scene", "render")

    private val FLAG = Regex("""--([a-z][a-z-]*)""")
    // A hot-path *compute* method (takes inputs) returning a fresh array violates the
    // out-param contract. Parameterless accessors (cached-array getters) are exempt.
    private val DOUBLE_ARRAY_RETURN = Regex("""\bdouble\[\]\s+\w+\s*\(\s*[A-Za-z][^)]*\)""")
    private val MATH_RANDOM = Regex("""Math\.random\s*\(""")
    private val THROW_VALIDATION = Regex("""throw\s+new\s+(IllegalArgumentException|IllegalStateException)""")
    private val PRINT_USAGE = Regex("""printUsage\s*=\s*true""")
    private val FILE_SINK = Regex("""\b(FileOutputStream|Files\.write|Files\.newOutputStream)\b""")
    private val STDOUT = Regex("""System\.(out|err)\b""")
    private val PARALLEL = Regex("""\.parallel\s*\(\s*\)""")
    private val TODO = Regex("""\b(TODO|FIXME|XXX)\b""")
    private val PUBLIC_MUTABLE_FIELD = Regex(
        """(?m)^\s*public\s+(?!static\s+final\b)(?!final\b)(?!abstract\b)(?!class\b)(?!interface\b)(?!enum\b)(?!record\b)[\w<>\[\].]+\s+\w+\s*(=[^;()]*)?;"""
    )
    private val PERLIN_SEED = Regex("""\b12345L\b""")
    private val ROW_SEED_PRIME = Regex("""0x9E3779B97F4A7C15L""")

    fun evaluate(inputs: ProjectInputs): Map<String, Measure> {
        val main = inputs.main
        val impls = MetricsCollector.implementationCounts(main)
        val mainText = main.joinToString("\n") { it.rawContent }
        val argsText = main.firstOrNull { it.path.fileName.toString() == "Args.java" }?.rawContent ?: ""
        val cov = inputs.coverage
        val tt = inputs.testTotals

        val sceneFormats = impls["SceneFormat"] ?: 0
        val imageWriters = impls["ImageWriter"] ?: 0
        val displays = impls["RenderDisplay"] ?: 0
        val gatePresent = inputs.buildScriptText.contains("verifyImage")

        val mainLoc = main.sumOf { it.loc }.coerceAtLeast(1)
        val testLoc = inputs.test.sumOf { it.loc }
        val meanFileLoc = mainLoc.toDouble() / main.size.coerceAtLeast(1)
        val filesOver250 = main.count { it.loc > 250 }
        val branchTotal = main.sumOf { it.branchingKeywordCount }
        val methodTotal = main.sumOf { it.methodCount }.coerceAtLeast(1)
        val branchingPerMethod = branchTotal.toDouble() / methodTotal

        val mainTypes = main.flatMap { it.types }
        val interfaceNames = mainTypes.filter { it.kind == TypeKind.INTERFACE }.map { it.name }
        val multiImpl = interfaceNames.count { (impls[it] ?: 0) >= 2 }
        val interfaceRatio = MetricsCollector.interfaceToTypeRatio(main)
        val coupling = MetricsCollector.packageMetrics(main).values
        val meanDistance = if (coupling.isEmpty()) 0.0 else coupling.map { it.distanceFromMainSequence }.average()
        val extensionPoints = extensionPointCount(main)

        val testMethods = inputs.test.sumOf { it.testMethodCount }
        val testDensity = if (mainTypes.isEmpty()) 0.0 else testMethods.toDouble() / mainTypes.size

        val hotPathAllocs = main.filter { it.types.any { t -> t.name in HOT_PATH_INTERFACES } }
            .sumOf { MetricsCollector.countMatches(it.rawContent, DOUBLE_ARRAY_RETURN) }
        val stdoutViolations = main.count { src ->
            LIBRARY_PACKAGE_SUFFIXES.any { src.packageName.endsWith(".$it") } &&
                STDOUT.containsMatchIn(src.rawContent)
        }
        val fileSinkViolations = main.count { src ->
            !src.packageName.endsWith(".io") && FILE_SINK.containsMatchIn(src.rawContent)
        }
        val mathRandomCount = MetricsCollector.countMatches(mainText, MATH_RANDOM)
        val publicMutableFields = main.sumOf { MetricsCollector.countMatches(it.rawContent, PUBLIC_MUTABLE_FIELD) }
        val validationCount = MetricsCollector.countMatches(mainText, THROW_VALIDATION) +
            MetricsCollector.countMatches(argsText, PRINT_USAGE)
        val todoCount = MetricsCollector.countMatches(mainText, TODO)

        val flagCount = flagSet(argsText).size
        val flagDiff = (flagSet(argsText) symmetricDiff flagSet(inputs.readmeText)).size

        val quirkViolations = listOf(PERLIN_SEED, ROW_SEED_PRIME).count { rx ->
            main.count { rx.containsMatchIn(it.rawContent) } != 1
        }

        val parallelPresent = PARALLEL.containsMatchIn(mainText)
        val quickPathPresent = argsText.contains("--quick")
        val installable = inputs.wrapperJarPresent && inputs.buildScriptText.contains("application")

        return mapOf(
            // 1. Functional Suitability
            "1.1" to measure(tt.passed.toDouble(), passRate(tt.passed, tt.total)),
            "1.2" to measure(bool(gatePresent), binary(gatePresent)),
            "1.3" to measure(sceneFormats.toDouble(), ratioToTarget(sceneFormats.toDouble(), 2.0)),
            "1.4" to measure(imageWriters.toDouble(), ratioToTarget(imageWriters.toDouble(), 3.0)),
            // 2. Performance Efficiency
            "2.1" to measure(bool(quickPathPresent), binary(quickPathPresent)),
            "2.2" to measure(bool(parallelPresent), binary(parallelPresent)),
            "2.3" to measure(hotPathAllocs.toDouble(), penaltyPerCount(hotPathAllocs, 50.0)),
            // 3. Compatibility
            "3.1" to measure(imageWriters.toDouble(), ratioToTarget(imageWriters.toDouble(), 3.0)),
            "3.2" to measure(sceneFormats.toDouble(), ratioToTarget(sceneFormats.toDouble(), 2.0)),
            "3.3" to measure(stdoutViolations.toDouble(), penaltyPerCount(stdoutViolations, 25.0)),
            // 4. Interaction Capability
            "4.1" to measure(flagCount.toDouble(), ratioToTarget(flagCount.toDouble(), 12.0)),
            "4.2" to measure(flagDiff.toDouble(), penaltyPerCount(flagDiff, 10.0)),
            "4.3" to measure(displays.toDouble(), ratioToTarget(displays.toDouble(), 2.0)),
            // 5. Reliability
            "5.1" to measure(tt.passed.toDouble(), passRate(tt.passed, tt.total)),
            "5.2" to measure(testLoc.toDouble() / mainLoc, ratioToTarget(testLoc.toDouble() / mainLoc, 0.5)),
            "5.3" to measure(validationCount.toDouble(), ratioToTarget(validationCount.toDouble(), 8.0)),
            "5.4" to measure(bool(gatePresent), binary(gatePresent)),
            "5.5" to measure(cov.lineRatio, ratioToTarget(cov.lineRatio, 0.70)),
            // 6. Security
            "6.1" to measure(fileSinkViolations.toDouble(), penaltyPerCount(fileSinkViolations, 50.0)),
            "6.2" to measure(mathRandomCount.toDouble(), binary(mathRandomCount == 0)),
            "6.3" to measure(publicMutableFields.toDouble(), penaltyPerCount(publicMutableFields, 5.0)),
            // 7. Maintainability
            "7.1" to measure(filesOver250.toDouble(), penaltyPerCount(filesOver250, 15.0)),
            "7.2" to measure(meanFileLoc, piecewiseLinear(meanFileLoc, 80.0, 200.0)),
            "7.3" to measure(meanDistance, invertedDistance(meanDistance)),
            "7.4" to measure(interfaceRatio, ratioToTarget(interfaceRatio, 0.20)),
            "7.5" to measure(branchingPerMethod, piecewiseLinear(branchingPerMethod, 3.0, 12.0)),
            "7.6" to measure(testDensity, ratioToTarget(testDensity, 1.5)),
            "7.7" to measure(cov.branchRatio, ratioToTarget(cov.branchRatio, 0.60)),
            // 8. Flexibility
            "8.1" to measure(multiImplRatio(interfaceNames, impls), ratioToTarget(multiImplRatio(interfaceNames, impls), 0.5)),
            "8.2" to measure(extensionPoints.toDouble(), ratioToTarget(extensionPoints.toDouble(), 10.0)),
            "8.3" to measure(MetricsCollector.totalSealedPermitsCount(main).toDouble(),
                ratioToTarget(MetricsCollector.totalSealedPermitsCount(main).toDouble(), 6.0)),
            "8.4" to measure(bool(installable), binary(installable)),
            "8.5" to measure(bool(parallelPresent), binary(parallelPresent)),
            // 9. Safety
            "9.1" to measure(bool(gatePresent), binary(gatePresent)),
            "9.2" to measure(todoCount.toDouble(), penaltyPerCount(todoCount, 10.0)),
            "9.3" to measure(quirkViolations.toDouble(), penaltyPerCount(quirkViolations, 33.0)),
        )
    }

    private fun measure(raw: Double, score: Double) = Measure(raw, score)
    private fun bool(b: Boolean): Double = if (b) 1.0 else 0.0

    private fun flagSet(text: String): Set<String> =
        FLAG.findAll(text).map { it.groupValues[1] }.toSet()

    private infix fun <T> Set<T>.symmetricDiff(other: Set<T>): Set<T> =
        (this - other) + (other - this)

    private fun multiImplRatio(interfaceNames: List<String>, impls: Map<String, Int>): Double {
        if (interfaceNames.isEmpty()) return 0.0
        val multi = interfaceNames.count { (impls[it] ?: 0) >= 2 }
        return multi.toDouble() / interfaceNames.size
    }

    private fun extensionPointCount(main: List<JavaSource>): Int =
        main.filter { src -> src.packageName.let { it.endsWith(".render") || it.endsWith(".io") || it.endsWith(".shading") } }
            .flatMap { it.types }
            .count { it.kind == TypeKind.INTERFACE }
}
