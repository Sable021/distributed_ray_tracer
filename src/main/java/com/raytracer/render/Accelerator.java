package com.raytracer.render;

import com.raytracer.Ray;
import com.raytracer.Scene;

/**
 * Spatial query for "given a ray, what is the nearest scene object it hits?".
 *
 * <p>The default {@link LinearAccelerator} runs a brute-force loop. A future BVH or
 * KD-tree implementation would slot in via the same interface; the renderer's hot
 * path doesn't change. The {@code depth == 1 && skipPrimaryRays} filter is part of
 * the contract, since it's a scene-query semantic — accelerators must honour it.
 */
public interface Accelerator {

    /**
     * Bind the accelerator to a scene, performing any precomputation
     * (e.g. building a BVH). Called once during {@link com.raytracer.Renderer}
     * construction.
     */
    void build(Scene scene);

    /**
     * Find the nearest scene object hit by {@code ray}.
     *
     * @param ray         ray to test
     * @param depth       current recursion depth; {@code depth == 1} (primary rays from
     *                    the camera) skips objects flagged
     *                    {@link com.raytracer.SceneObject#skipPrimaryRays}
     * @param outIntersect caller-owned RGB[3]-size buffer; on a hit, written with the
     *                     world-space intersection point
     * @return scene-object index of the nearest hit, or {@code -1} on miss
     */
    int nearest(Ray ray, int depth, double[] outIntersect);
}
