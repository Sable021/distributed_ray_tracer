package com.raytracer.ci

import kotlin.math.max
import kotlin.math.min

object ScoringFunctions {

    fun ratioToTarget(value: Double, target: Double): Double {
        if (target <= 0.0) return 0.0
        val capped = min(1.0, max(0.0, value / target))
        return 100.0 * capped
    }

    fun passRate(passed: Int, total: Int): Double {
        if (total <= 0) return 0.0
        val clamped = min(total, max(0, passed))
        return 100.0 * clamped.toDouble() / total.toDouble()
    }

    fun binary(condition: Boolean): Double = if (condition) 100.0 else 0.0

    fun penaltyPerCount(count: Int, penaltyPerUnit: Double): Double {
        val effective = max(0, count)
        return max(0.0, 100.0 - penaltyPerUnit * effective)
    }

    fun invertedDistance(value: Double): Double {
        val clamped = min(1.0, max(0.0, value))
        return 100.0 - 100.0 * clamped
    }

    fun piecewiseLinear(value: Double, floor: Double, ceiling: Double): Double {
        require(ceiling > floor) { "ceiling ($ceiling) must exceed floor ($floor)" }
        if (value <= floor) return 100.0
        if (value >= ceiling) return 0.0
        return 100.0 * (ceiling - value) / (ceiling - floor)
    }
}
