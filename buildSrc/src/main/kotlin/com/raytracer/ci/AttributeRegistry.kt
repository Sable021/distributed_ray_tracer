package com.raytracer.ci

data class Indicator(
    val id: String,
    val name: String,
    val subCharacteristic: String,
    val weight: Int,
)

data class Attribute(
    val id: String,
    val displayName: String,
    val weight: Int,
    val indicators: List<Indicator>,
)

object AttributeRegistry {

    val attributes: List<Attribute> = listOf(
        Attribute(
            id = "functionalSuitability", displayName = "Functional Suitability", weight = 2,
            indicators = listOf(
                Indicator("1.1", "Test pass rate", "Correctness", weight = 3),
                Indicator("1.2", "Golden-hash parity", "Correctness", weight = 3),
                Indicator("1.3", "Scene-format completeness", "Completeness", weight = 2),
                Indicator("1.4", "Output-format completeness", "Appropriateness", weight = 2),
            ),
        ),
        Attribute(
            id = "performanceEfficiency", displayName = "Performance Efficiency", weight = 1,
            indicators = listOf(
                Indicator("2.1", "Quick smoke render under budget", "Time Behaviour", weight = 1),
                Indicator("2.2", "Parallel render present", "Capacity", weight = 2),
                Indicator("2.3", "No new allocations in trace hot path", "Resource Utilization", weight = 2),
            ),
        ),
        Attribute(
            id = "compatibility", displayName = "Compatibility", weight = 1,
            indicators = listOf(
                Indicator("3.1", "Output-format breadth", "Interoperability", weight = 2),
                Indicator("3.2", "Scene-format breadth", "Interoperability", weight = 2),
                Indicator("3.3", "Co-existence isolation (no stdout outside io/composition root)", "Co-existence", weight = 1),
            ),
        ),
        Attribute(
            id = "interactionCapability", displayName = "Interaction Capability", weight = 1,
            indicators = listOf(
                Indicator("4.1", "CLI flag count", "Operability", weight = 1),
                Indicator("4.2", "README ↔ Args flag parity", "Learnability", weight = 2),
                Indicator("4.3", "Display mode count", "Appropriateness Recognizability", weight = 1),
            ),
        ),
        Attribute(
            id = "reliability", displayName = "Reliability", weight = 2,
            indicators = listOf(
                Indicator("5.1", "Test suite pass rate", "Faultlessness", weight = 3),
                Indicator("5.2", "Test-to-production LOC ratio", "Fault Tolerance", weight = 1),
                Indicator("5.3", "Boundary validation density", "Fault Tolerance", weight = 2),
                Indicator("5.4", "Regression gate present (verifyImage)", "Availability", weight = 2),
                Indicator("5.5", "JaCoCo line coverage", "Faultlessness", weight = 3),
            ),
        ),
        Attribute(
            id = "security", displayName = "Security", weight = 1,
            indicators = listOf(
                Indicator("6.1", "File I/O routed through ImageWriter boundary", "Integrity", weight = 1),
                Indicator("6.2", "No Math.random in production", "Integrity", weight = 2),
                Indicator("6.3", "No mutable public fields on data carriers", "Integrity", weight = 2),
            ),
        ),
        Attribute(
            id = "maintainability", displayName = "Maintainability", weight = 3,
            indicators = listOf(
                Indicator("7.1", "Files ≤ 250 LOC", "Modularity", weight = 2),
                Indicator("7.2", "Mean file LOC", "Analysability", weight = 2),
                Indicator("7.3", "Package coupling distance from main sequence", "Modularity", weight = 3),
                Indicator("7.4", "Interface-to-type ratio", "Reusability", weight = 2),
                Indicator("7.5", "Branching-density proxy", "Analysability", weight = 2),
                Indicator("7.6", "Test method density", "Testability", weight = 3),
                Indicator("7.7", "JaCoCo branch coverage", "Testability", weight = 3),
            ),
        ),
        Attribute(
            id = "flexibility", displayName = "Flexibility", weight = 3,
            indicators = listOf(
                Indicator("8.1", "Multi-impl interface ratio", "Replaceability", weight = 3),
                Indicator("8.2", "Strategy/observer extension points", "Adaptability", weight = 2),
                Indicator("8.3", "Sealed-hierarchy variant count", "Adaptability", weight = 2),
                Indicator("8.4", "Installable via Gradle wrapper", "Installability", weight = 1),
                Indicator("8.5", "Parallel scalability", "Scalability", weight = 2),
            ),
        ),
        Attribute(
            id = "safety", displayName = "Safety", weight = 1,
            indicators = listOf(
                Indicator("9.1", "Golden-hash gate present", "Fail Safe", weight = 2),
                Indicator("9.2", "TODO/FIXME count", "Hazard Warning", weight = 1),
                Indicator("9.3", "C++-quirk owners single-sourced", "Operational Constraint", weight = 2),
            ),
        ),
    )

    fun sumWeights(): Int = attributes.sumOf { it.weight }

    fun allIndicators(): List<Indicator> = attributes.flatMap { it.indicators }

    fun byId(id: String): Attribute =
        attributes.firstOrNull { it.id == id }
            ?: error("unknown attribute id: $id (known: ${attributes.map { it.id }})")
}
