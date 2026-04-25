package com.raytracer;

import java.util.SplittableRandom;

/**
 * Deterministic, thread-aware random number source for the renderer.
 *
 * <p>Backed by a {@link ThreadLocal} {@link SplittableRandom} so each worker thread has
 * its own stream. {@link #reseed(long)} is called at the start of each scanline with a
 * row-derived seed; this guarantees byte-identical output across runs regardless of
 * which thread happens to render which row, which is the property that makes parallel
 * rendering deterministic.
 *
 * <p>Used to jitter supersample positions, area-light sub-samples, and glossy reflection
 * grid offsets.
 */
public final class Rng {

    /** Per-thread RNG. Initial seed is replaced by {@link #reseed} at the start of every row. */
    private static final ThreadLocal<SplittableRandom> RNG =
            ThreadLocal.withInitial(() -> new SplittableRandom(1_234_567_890L));

    private Rng() {}

    /**
     * Replace the calling thread's RNG with a fresh {@code SplittableRandom} seeded with
     * the given value. The renderer calls this at the top of each scanline so the per-row
     * sequence depends only on the row index, not on thread scheduling.
     */
    public static void reseed(long seed) {
        RNG.set(new SplittableRandom(seed));
    }

    /** Return a uniformly distributed sample in [min, max) from the calling thread's RNG. */
    public static double uniform(double min, double max) {
        return min + RNG.get().nextDouble() * (max - min);
    }
}
