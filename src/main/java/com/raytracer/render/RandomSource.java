package com.raytracer.render;

/**
 * Uniform random source, abstracted for dependency injection so the renderer's
 * RNG choice (currently {@link ThreadLocalRandomSource}, backed by
 * {@link java.util.SplittableRandom SplittableRandom}) can be swapped without
 * touching call sites in {@link com.raytracer.Renderer}, {@link com.raytracer.RayTracer},
 * or {@link com.raytracer.shading.AreaLight}.
 *
 * <p>The renderer's per-row reseeding makes parallel rendering deterministic: each
 * row gets a fresh seed derived only from the row index, so byte-identical PPM
 * output is reproduced regardless of which thread renders which row.
 */
public interface RandomSource {

    /**
     * Replace the calling thread's RNG with a fresh one seeded with {@code seed}.
     * Called at the top of every scanline.
     */
    void reseed(long seed);

    /** Return a uniformly distributed sample in {@code [lo, hi)} from the calling thread's RNG. */
    double uniform(double lo, double hi);
}
