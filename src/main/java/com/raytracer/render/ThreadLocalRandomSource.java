package com.raytracer.render;

import java.util.SplittableRandom;

/**
 * {@link RandomSource} backed by a {@link ThreadLocal} {@link SplittableRandom} so each
 * worker thread has its own stream. Behaviourally identical to the pre-Phase-D static
 * {@code Rng} class — this is the lone owner of the {@code SplittableRandom}-equivalence
 * requirement that keeps PPM hashes stable.
 */
public final class ThreadLocalRandomSource implements RandomSource {

    /** Initial per-thread seed. Replaced by {@link #reseed} at the start of every row. */
    private static final long INITIAL_SEED = 1_234_567_890L;

    private final ThreadLocal<SplittableRandom> rng =
            ThreadLocal.withInitial(() -> new SplittableRandom(INITIAL_SEED));

    @Override
    public void reseed(long seed) {
        rng.set(new SplittableRandom(seed));
    }

    @Override
    public double uniform(double lo, double hi) {
        return lo + rng.get().nextDouble() * (hi - lo);
    }
}
