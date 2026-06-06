package com.raytracer.ci

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ScoringFunctionsTest {

    private val eps = 1e-9

    @Test
    fun ratioToTargetIsZeroAtZero() {
        assertEquals(0.0, ScoringFunctions.ratioToTarget(0.0, 10.0), eps)
    }

    @Test
    fun ratioToTargetIsHalfAtHalfTarget() {
        assertEquals(50.0, ScoringFunctions.ratioToTarget(5.0, 10.0), eps)
    }

    @Test
    fun ratioToTargetCapsAtOneHundredAtTarget() {
        assertEquals(100.0, ScoringFunctions.ratioToTarget(10.0, 10.0), eps)
    }

    @Test
    fun ratioToTargetStaysCappedAboveTarget() {
        assertEquals(100.0, ScoringFunctions.ratioToTarget(50.0, 10.0), eps)
    }

    @Test
    fun ratioToTargetReturnsZeroWhenTargetIsZero() {
        assertEquals(0.0, ScoringFunctions.ratioToTarget(5.0, 0.0), eps)
    }

    @Test
    fun passRateAllPassed() {
        assertEquals(100.0, ScoringFunctions.passRate(92, 92), eps)
    }

    @Test
    fun passRateHalfPassed() {
        assertEquals(50.0, ScoringFunctions.passRate(46, 92), eps)
    }

    @Test
    fun passRateNonePassed() {
        assertEquals(0.0, ScoringFunctions.passRate(0, 92), eps)
    }

    @Test
    fun passRateZeroTotalIsZero() {
        assertEquals(0.0, ScoringFunctions.passRate(0, 0), eps)
    }

    @Test
    fun binaryTrueIsOneHundred() {
        assertEquals(100.0, ScoringFunctions.binary(true), eps)
    }

    @Test
    fun binaryFalseIsZero() {
        assertEquals(0.0, ScoringFunctions.binary(false), eps)
    }

    @Test
    fun penaltyPerCountZeroIsPerfect() {
        assertEquals(100.0, ScoringFunctions.penaltyPerCount(0, 15.0), eps)
    }

    @Test
    fun penaltyPerCountOneViolation() {
        assertEquals(85.0, ScoringFunctions.penaltyPerCount(1, 15.0), eps)
    }

    @Test
    fun penaltyPerCountFloorsAtZero() {
        assertEquals(0.0, ScoringFunctions.penaltyPerCount(10, 15.0), eps)
    }

    @Test
    fun invertedDistanceZeroIsOneHundred() {
        assertEquals(100.0, ScoringFunctions.invertedDistance(0.0), eps)
    }

    @Test
    fun invertedDistanceHalfIsFifty() {
        assertEquals(50.0, ScoringFunctions.invertedDistance(0.5), eps)
    }

    @Test
    fun invertedDistanceOneIsZero() {
        assertEquals(0.0, ScoringFunctions.invertedDistance(1.0), eps)
    }

    @Test
    fun invertedDistanceClampsBelowZero() {
        assertEquals(0.0, ScoringFunctions.invertedDistance(2.0), eps)
    }

    @Test
    fun piecewiseAtOrBelowFloorIsOneHundred() {
        assertEquals(100.0, ScoringFunctions.piecewiseLinear(2.0, floor = 5.0, ceiling = 30.0), eps)
        assertEquals(100.0, ScoringFunctions.piecewiseLinear(5.0, floor = 5.0, ceiling = 30.0), eps)
    }

    @Test
    fun piecewiseAtOrAboveCeilingIsZero() {
        assertEquals(0.0, ScoringFunctions.piecewiseLinear(30.0, floor = 5.0, ceiling = 30.0), eps)
        assertEquals(0.0, ScoringFunctions.piecewiseLinear(50.0, floor = 5.0, ceiling = 30.0), eps)
    }

    @Test
    fun piecewiseAtMidpointIsFifty() {
        // floor=5, ceiling=30, midpoint=17.5
        assertEquals(50.0, ScoringFunctions.piecewiseLinear(17.5, floor = 5.0, ceiling = 30.0), eps)
    }

    @Test
    fun piecewiseMonotonicallyDecreasing() {
        val a = ScoringFunctions.piecewiseLinear(10.0, floor = 5.0, ceiling = 30.0)
        val b = ScoringFunctions.piecewiseLinear(20.0, floor = 5.0, ceiling = 30.0)
        assertTrue(a > b, "expected score to decrease as input rises: a=$a b=$b")
    }

    @Test
    fun everyFunctionStaysInZeroOneHundredRange() {
        val samples = listOf(
            ScoringFunctions.ratioToTarget(-5.0, 10.0),
            ScoringFunctions.ratioToTarget(1000.0, 10.0),
            ScoringFunctions.passRate(-1, 10),
            ScoringFunctions.passRate(100, 10),
            ScoringFunctions.penaltyPerCount(-3, 15.0),
            ScoringFunctions.penaltyPerCount(9999, 15.0),
            ScoringFunctions.invertedDistance(-0.5),
            ScoringFunctions.invertedDistance(5.0),
            ScoringFunctions.piecewiseLinear(-100.0, 5.0, 30.0),
            ScoringFunctions.piecewiseLinear(9999.0, 5.0, 30.0),
        )
        samples.forEach { v ->
            assertTrue(v in 0.0..100.0, "scoring function out of range: $v")
        }
    }
}
