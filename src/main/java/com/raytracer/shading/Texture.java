package com.raytracer.shading;

/**
 * Surface albedo function evaluated at a world-space point.
 *
 * <p>Implementations write three RGB components in [0,1] into {@code outRgb}; they must not
 * allocate. {@link Material#albedo()} returns one of these — the shading pipeline samples
 * it once per shadow ray.
 */
public interface Texture {
    /** Sample the texture at {@code point} and write RGB into {@code outRgb}. */
    void sample(double[] point, double[] outRgb);
}
