package com.raytracer.shading;

import com.raytracer.render.RandomSource;
import com.raytracer.render.Sampler;
import com.raytracer.render.StratifiedSampler;
import com.raytracer.render.ThreadLocalRandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AreaLightTest {

    /** Unit square in the y=10 plane, corners CCW from origin (1×1 quad). */
    private static double[][] unitSquare() {
        return new double[][]{
            {0, 10, 0},
            {1, 10, 0},
            {1, 10, 1},
            {0, 10, 1}
        };
    }

    @Test
    void shadowCasterScanLimitMatchesSceneSize() {
        AreaLight al = new AreaLight(
                new double[]{1, 1, 1},
                new double[]{1, 1, 1},
                unitSquare());

        assertEquals(17, al.shadowCasterScanLimit(17));
        assertEquals(99, al.shadowCasterScanLimit(99));
    }

    @Test
    void sampleCountFollowsConfigured() {
        AreaLight al = new AreaLight(
                new double[]{1, 1, 1},
                new double[]{1, 1, 1},
                unitSquare());

        assertEquals(4,  al.sampleCount(4));
        assertEquals(16, al.sampleCount(16));
    }

    /**
     * Without buildGrid, sampling would NPE — guards that callers must initialise the grid
     * before any rayTrace call. {@link com.raytracer.Renderer} does this in its constructor.
     */
    @Test
    void samplePositionRequiresBuildGridFirst() {
        AreaLight al = new AreaLight(
                new double[]{1, 1, 1},
                new double[]{1, 1, 1},
                unitSquare());
        RandomSource rng = new ThreadLocalRandomSource();
        Sampler sampler = new StratifiedSampler();

        assertThrows(RuntimeException.class,
                     () -> al.samplePosition(0, 0, rng, sampler, new double[3]));
    }

    /**
     * After buildGrid, samples should fall within the quad's axis-aligned bounding extent
     * (in our unit-square test fixture: [0,1] × y=10 × [0,1]).
     */
    @Test
    void samplesFallWithinTheQuadExtent() {
        AreaLight al = new AreaLight(
                new double[]{1, 1, 1},
                new double[]{1, 1, 1},
                unitSquare());
        al.buildGrid(4, 4);

        // Use a fixed seed so this test is deterministic regardless of test ordering.
        RandomSource rng = new ThreadLocalRandomSource();
        rng.reseed(0xDEADBEEFL);
        Sampler sampler = new StratifiedSampler();
        double[] out = new double[3];

        for (int rayNum = 0; rayNum < 16; rayNum++) {
            for (int k = 0; k < 4; k++) {
                al.samplePosition(k, rayNum, rng, sampler, out);
                assertTrue(out[0] >= 0.0 && out[0] <= 1.0, "x in [0,1]: " + out[0]);
                assertEquals(10.0, out[1], 1e-12);
                assertTrue(out[2] >= 0.0 && out[2] <= 1.0, "z in [0,1]: " + out[2]);
            }
        }
    }
}
