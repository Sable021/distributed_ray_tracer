package com.raytracer.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StratifiedSamplerTest {

    /** Reference computation matching the pre-refactor {@code Sampling.getGridNumber}. */
    private static int[] referenceCellForRay(int rayNum, int gridX, int gridY) {
        int gridSize = gridX * gridY;
        int gridNum  = (rayNum * 7) % gridSize;
        return new int[]{ gridNum % gridX, gridNum / gridX };
    }

    /**
     * For all {@code rayNum} values the renderer ever produces in a single pixel
     * (up to 8x8 = 64 supersample rays), the new sampler must reproduce the exact
     * cell choice of the pre-refactor stratification logic. This is the
     * equivalence that keeps PPM hashes stable across the Phase D refactor.
     */
    @Test
    void cellForRayMatchesPreRefactorReferenceForAllSupersampleSizes() {
        StratifiedSampler s = new StratifiedSampler();

        int[][] grids = { {1, 1}, {2, 2}, {4, 4}, {8, 8}, {3, 5}, {5, 3} };
        for (int[] g : grids) {
            int gx = g[0], gy = g[1];
            for (int rayNum = 0; rayNum < gx * gy; rayNum++) {
                int[] expected = referenceCellForRay(rayNum, gx, gy);
                int[] actual   = s.cellForRay(rayNum, gx, gy);
                assertArrayEquals(expected, actual,
                        String.format("grid %dx%d, rayNum=%d", gx, gy, rayNum));
            }
        }
    }

    @Test
    void cellForRayStaysInsideTheGrid() {
        StratifiedSampler s = new StratifiedSampler();
        int gx = 8, gy = 8;
        for (int rayNum = 0; rayNum < gx * gy; rayNum++) {
            int[] xy = s.cellForRay(rayNum, gx, gy);
            assertTrue(xy[0] >= 0 && xy[0] < gx, "x bound: " + xy[0]);
            assertTrue(xy[1] >= 0 && xy[1] < gy, "y bound: " + xy[1]);
        }
    }

    /**
     * The {@code *7} multiplier is coprime to common N×N grids so consecutive ray
     * indices visit distinct cells until the grid is fully covered. This is the
     * stratification property that the C++ original relied on.
     */
    @Test
    void consecutiveRayIndicesVisitDistinctCellsForOctaveGrid() {
        StratifiedSampler s = new StratifiedSampler();
        int gx = 8, gy = 8;
        boolean[][] seen = new boolean[gy][gx];
        for (int rayNum = 0; rayNum < gx * gy; rayNum++) {
            int[] xy = s.cellForRay(rayNum, gx, gy);
            assertFalse(seen[xy[1]][xy[0]],
                    "cell (" + xy[0] + "," + xy[1] + ") visited twice within one period");
            seen[xy[1]][xy[0]] = true;
        }
    }
}
