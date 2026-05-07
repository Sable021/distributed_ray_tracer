package com.raytracer.render;

import com.raytracer.Ray;

/**
 * Computes the radiance seen along a ray. Decouples per-pixel ray construction
 * (handled by {@link RenderStrategy}) from the recursive shading algorithm
 * (Phong + reflection + refraction in the production implementation, but free to be
 * swapped for a path tracer, photon mapper, debug AOV, or test stub).
 */
public interface PathIntegrator {

    /**
     * Compute the colour seen along {@code ray} and accumulate it into {@code outColour}.
     *
     * @param ray       primary or secondary ray to trace
     * @param depth     current recursion depth (primary rays start at 1)
     * @param rindex    refractive index of the medium the ray is currently inside
     * @param outColour caller-owned RGB[3] receiving the shaded colour (overwritten)
     * @param inside    true if the ray is travelling inside a refractive object
     * @param rayNum    sample index used to deterministically pick a stratified sub-cell
     */
    void trace(Ray ray, int depth, double rindex,
               double[] outColour, boolean inside, int rayNum);
}
