package com.raytracer.render;

import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.*;

class ThreadLocalRandomSourceTest {

    /**
     * After {@link RandomSource#reseed} the per-thread stream must reproduce the same
     * sequence as a fresh {@link SplittableRandom} with that seed, scaled and offset by
     * {@code uniform(lo, hi)}'s contract. This is the equivalence that keeps the
     * post-refactor PPM hashes byte-identical.
     */
    @Test
    void uniformAfterReseedMatchesSplittableRandomReference() {
        long seed = 0xCAFEBABEL;
        RandomSource rs = new ThreadLocalRandomSource();
        rs.reseed(seed);
        SplittableRandom ref = new SplittableRandom(seed);

        for (int i = 0; i < 1024; i++) {
            double expected = 0.0 + ref.nextDouble() * (1.0 - 0.0);
            double actual   = rs.uniform(0.0, 1.0);
            assertEquals(expected, actual, 0.0, "iteration " + i);
        }
    }

    @Test
    void uniformRespectsLowerAndUpperBounds() {
        RandomSource rs = new ThreadLocalRandomSource();
        rs.reseed(7L);

        for (int i = 0; i < 1024; i++) {
            double v = rs.uniform(2.5, 7.5);
            assertTrue(v >= 2.5 && v < 7.5, "uniform must respect [lo, hi): " + v);
        }
    }

    @Test
    void reseedingResetsTheStream() {
        RandomSource rs = new ThreadLocalRandomSource();

        rs.reseed(42L);
        double[] first = new double[8];
        for (int i = 0; i < 8; i++) first[i] = rs.uniform(0, 1);

        rs.reseed(42L);
        for (int i = 0; i < 8; i++) {
            assertEquals(first[i], rs.uniform(0, 1), 0.0,
                         "stream must repeat exactly after reseed");
        }
    }
}
