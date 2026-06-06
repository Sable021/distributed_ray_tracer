package com.raytracer.ci

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class AttributeRegistryTest {

    @Test
    fun topLevelWeightsSumToFifteen() {
        assertEquals(15, AttributeRegistry.sumWeights())
    }

    @Test
    fun allNineIso25010AttributesArePresent() {
        val ids = AttributeRegistry.attributes.map { it.id }.toSet()
        val expected = setOf(
            "functionalSuitability", "performanceEfficiency", "compatibility",
            "interactionCapability", "reliability", "security",
            "maintainability", "flexibility", "safety",
        )
        assertEquals(expected, ids)
    }

    @Test
    fun maintainabilityAndFlexibilityAreTheHeaviest() {
        val m = AttributeRegistry.byId("maintainability").weight
        val f = AttributeRegistry.byId("flexibility").weight
        assertEquals(3, m)
        assertEquals(3, f)
    }

    @Test
    fun reliabilityAndFunctionalSuitabilityAreWeightTwo() {
        assertEquals(2, AttributeRegistry.byId("reliability").weight)
        assertEquals(2, AttributeRegistry.byId("functionalSuitability").weight)
    }

    @Test
    fun everyAttributeHasAtLeastTwoIndicators() {
        AttributeRegistry.attributes.forEach { attr ->
            assertTrue(
                attr.indicators.size >= 2,
                "attribute ${attr.id} has only ${attr.indicators.size} indicator(s)",
            )
        }
    }

    @Test
    fun everyIndicatorHasPositiveWeight() {
        AttributeRegistry.allIndicators().forEach { ind ->
            assertTrue(ind.weight > 0, "indicator ${ind.id} has weight ${ind.weight}")
        }
    }

    @Test
    fun indicatorIdsAreUnique() {
        val ids = AttributeRegistry.allIndicators().map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate indicator IDs: $ids")
    }

    @Test
    fun indicatorIdsFollowAttributeNumberingScheme() {
        AttributeRegistry.attributes.forEachIndexed { index, attr ->
            val expectedPrefix = "${index + 1}."
            attr.indicators.forEach { ind ->
                assertTrue(
                    ind.id.startsWith(expectedPrefix),
                    "indicator ${ind.id} should start with $expectedPrefix (attribute ${attr.id})",
                )
            }
        }
    }

    @Test
    fun reliabilityIncludesJaCoCoLineCoverage() {
        val ids = AttributeRegistry.byId("reliability").indicators.map { it.id }
        assertTrue("5.5" in ids, "reliability missing JaCoCo line-coverage indicator (5.5): $ids")
    }

    @Test
    fun maintainabilityIncludesJaCoCoBranchCoverage() {
        val ids = AttributeRegistry.byId("maintainability").indicators.map { it.id }
        assertTrue("7.7" in ids, "maintainability missing JaCoCo branch-coverage indicator (7.7): $ids")
    }
}
