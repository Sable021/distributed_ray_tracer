package com.raytracer.render;

/**
 * Maps a 1-D ray index to a 2-D grid cell. Used by glossy reflection and area-light
 * shadow sampling to spread adjacent rays across the supersample grid.
 *
 * <p>Implementations decide the stratification policy. The default
 * {@link StratifiedSampler} preserves the C++ {@code *7} scrambler.
 */
public interface Sampler {

    /**
     * @param rayNum sequential index of the ray within a pixel's supersample budget
     * @param gridX  grid columns
     * @param gridY  grid rows
     * @return {@code int[]{x, y}} cell coordinates in {@code [0, gridX) × [0, gridY)}
     */
    int[] cellForRay(int rayNum, int gridX, int gridY);
}
