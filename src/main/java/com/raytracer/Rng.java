package com.raytracer;

import java.util.SplittableRandom;

/**
 * Deterministic random number source for the renderer.
 *
 * <p>Backed by a single {@link SplittableRandom} seeded with a fixed constant so that the
 * same image is produced byte-for-byte across JVM runs. Used to jitter supersample positions,
 * area-light sub-samples, and glossy reflection grid offsets.
 *
 * <p>Note: not thread-safe. The current renderer is single-threaded; if rendering is
 * parallelised, each worker should hold its own split RNG.
 */
public final class Rng {

    private static final SplittableRandom RNG = new SplittableRandom(1_234_567_890L);

    private Rng() {}

    /** Return a uniformly distributed sample in [min, max). */
    public static double uniform(double min, double max) {
        return min + RNG.nextDouble() * (max - min);
    }
}
