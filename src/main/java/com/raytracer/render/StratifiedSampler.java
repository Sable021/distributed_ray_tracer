package com.raytracer.render;

/**
 * {@link Sampler} that scrambles consecutive ray indices via the C++ original's
 * {@code *7} multiplier. This class is the lone owner of that quirk — preserving
 * it here is what keeps the post-refactor PPM hashes byte-identical to the
 * pre-refactor reference render.
 *
 * <p>The {@code *7} factor coprime to typical small grid sizes ({@code N×N} with
 * {@code N ∈ {1, 2, 4, 8, 16}}) so consecutive ray numbers visit distinct cells
 * before wrapping. The cell decomposition uses {@code gridX} (cols) for the
 * row-major split, matching the C++ behaviour for both square and non-square grids.
 */
public final class StratifiedSampler implements Sampler {

    @Override
    public int[] cellForRay(int rayNum, int gridX, int gridY) {
        int gridSize = gridX * gridY;
        int gridNum  = (rayNum * 7) % gridSize;   // preserve the magic 7 multiplier
        // Row-major decomposition: x = idx % cols, y = idx / cols (cols = gridX).
        // The C++ original used /gridY here; harmless for square grids but wrong otherwise.
        return new int[]{ gridNum % gridX, gridNum / gridX };
    }
}
