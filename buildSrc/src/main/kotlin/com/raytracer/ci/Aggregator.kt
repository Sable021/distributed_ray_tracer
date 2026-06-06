package com.raytracer.ci

data class Measure(val raw: Double, val score: Double)

data class IndicatorScore(
    val id: String,
    val name: String,
    val raw: Double,
    val score: Double,
    val weight: Int,
)

data class AttributeScore(
    val id: String,
    val displayName: String,
    val weight: Int,
    val score: Double,
    val indicators: List<IndicatorScore>,
)

data class ChangeabilityReport(
    val index: Double,
    val computedAt: String,
    val attributes: List<AttributeScore>,
)

object Aggregator {

    fun aggregate(measures: Map<String, Measure>, computedAt: String = ""): ChangeabilityReport {
        val attributes = AttributeRegistry.attributes.map { attr ->
            val indicators = attr.indicators.map { ind ->
                val m = measures[ind.id]
                IndicatorScore(ind.id, ind.name, m?.raw ?: 0.0, m?.score ?: 0.0, ind.weight)
            }
            val weightSum = attr.indicators.sumOf { it.weight }
            val score = indicators.sumOf { it.score * it.weight } / weightSum
            AttributeScore(attr.id, attr.displayName, attr.weight, score, indicators)
        }
        val index = (attributes.sumOf { it.score * it.weight } / AttributeRegistry.sumWeights())
            .coerceIn(1.0, 100.0)
        return ChangeabilityReport(index, computedAt, attributes)
    }
}
