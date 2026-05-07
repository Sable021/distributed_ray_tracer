package com.raytracer.render;

import com.raytracer.Ray;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@link PinholeStrategy} is responsible for constructing primary rays from a fixed
 * eye through jittered points on the screen-plane cell, and averaging the integrator's
 * returned radiance over the {@code gridX * gridY} samples. These tests pin those two
 * behaviours independently of any real shading model by injecting a recording
 * {@link PathIntegrator} fake.
 */
class PinholeStrategyTest {

    /**
     * Records every ray it sees and writes a deterministic colour back to the caller —
     * one ray index → one RGB triple, so the strategy's averaging arithmetic is
     * verifiable from outside.
     */
    private static final class RecordingIntegrator implements PathIntegrator {
        final List<double[]> origins = new ArrayList<>();
        final List<double[]> dirs    = new ArrayList<>();
        final List<Integer>  rayNums = new ArrayList<>();
        double colourR = 0, colourG = 0, colourB = 0;

        @Override
        public void trace(Ray ray, int depth, double rindex,
                          double[] outColour, boolean inside, int rayNum) {
            origins.add(ray.point.clone());
            dirs.add(ray.direct.clone());
            rayNums.add(rayNum);
            outColour[0] = colourR;
            outColour[1] = colourG;
            outColour[2] = colourB;
        }
    }

    private static final double[] EYE   = {0.0, 0.0, 5.0};
    private static final double   SCR_Z = 0.0;

    @Test
    void shadePixelWithGrid1x1FiresOneRayFromEyeThroughTheCell() {
        RecordingIntegrator integrator = new RecordingIntegrator();
        ThreadLocalRandomSource rng = new ThreadLocalRandomSource();
        rng.reseed(42);
        PinholeStrategy s = new PinholeStrategy(EYE, SCR_Z, integrator, rng);

        double[] rgb = new double[3];
        s.shadePixel(-0.5, -0.5, 1.0, 1.0, 1, 1, rgb);

        assertEquals(1, integrator.origins.size(), "1x1 grid must fire exactly one ray");
        assertArrayEquals(EYE, integrator.origins.get(0), 0.0,
                "ray origin must equal the eye, byte-for-byte");
        // Sub-pixel point sits inside the cell [-0.5, 0.5)
        assertTrue(integrator.dirs.get(0)[2] < 0, "ray must travel in -z toward the screen plane");
    }

    @Test
    void shadePixelAccumulatesGridXTimesGridYRays() {
        RecordingIntegrator integrator = new RecordingIntegrator();
        ThreadLocalRandomSource rng = new ThreadLocalRandomSource();
        rng.reseed(123);
        PinholeStrategy s = new PinholeStrategy(EYE, SCR_Z, integrator, rng);

        double[] rgb = new double[3];
        s.shadePixel(-0.5, -0.5, 1.0, 1.0, 4, 4, rgb);

        assertEquals(16, integrator.origins.size(), "4x4 grid must fire 16 rays");
        // rayNum runs 0..gridSize-1 in row-major order: k*gridX+l
        for (int i = 0; i < 16; i++) {
            assertEquals(i, integrator.rayNums.get(i),
                    "rayNum sequence must be 0..gridSize-1 in row-major order");
        }
    }

    @Test
    void shadePixelAveragesIntegratorOutputAcrossAllSamples() {
        RecordingIntegrator integrator = new RecordingIntegrator();
        integrator.colourR = 0.6;
        integrator.colourG = 0.4;
        integrator.colourB = 0.2;
        ThreadLocalRandomSource rng = new ThreadLocalRandomSource();
        rng.reseed(7);
        PinholeStrategy s = new PinholeStrategy(EYE, SCR_Z, integrator, rng);

        double[] rgb = new double[3];
        s.shadePixel(0.0, 0.0, 1.0, 1.0, 4, 4, rgb);

        // every ray returns (0.6, 0.4, 0.2); average over 16 samples must be (0.6, 0.4, 0.2)
        assertArrayEquals(new double[]{0.6, 0.4, 0.2}, rgb, 1e-12);
    }

    @Test
    void allRayOriginsEqualTheEyeRegardlessOfGridSize() {
        RecordingIntegrator integrator = new RecordingIntegrator();
        ThreadLocalRandomSource rng = new ThreadLocalRandomSource();
        rng.reseed(99);
        PinholeStrategy s = new PinholeStrategy(EYE, SCR_Z, integrator, rng);

        double[] rgb = new double[3];
        s.shadePixel(-1.0, -2.0, 0.5, 0.25, 8, 8, rgb);

        for (double[] origin : integrator.origins) {
            assertArrayEquals(EYE, origin, 0.0, "every primary ray origin must equal the eye");
        }
    }

    @Test
    void allSubSamplePointsLandInsideThePixelCell() {
        RecordingIntegrator integrator = new RecordingIntegrator();
        ThreadLocalRandomSource rng = new ThreadLocalRandomSource();
        rng.reseed(2026);
        double scrX = 1.0, scrY = 2.0, scrDX = 0.5, scrDY = 0.25;
        PinholeStrategy s = new PinholeStrategy(EYE, SCR_Z, integrator, rng);

        double[] rgb = new double[3];
        s.shadePixel(scrX, scrY, scrDX, scrDY, 4, 4, rgb);

        // Reconstruct the screen point each ray was aimed at: eye + t*dir at z = SCR_Z.
        // Then assert it falls inside [scrX, scrX+scrDX] x [scrY, scrY+scrDY].
        for (double[] dir : integrator.dirs) {
            double t = (SCR_Z - EYE[2]) / dir[2];
            double x = EYE[0] + t * dir[0];
            double y = EYE[1] + t * dir[1];
            assertTrue(x >= scrX && x <= scrX + scrDX,
                    "screen-plane x out of cell: " + x);
            assertTrue(y >= scrY && y <= scrY + scrDY,
                    "screen-plane y out of cell: " + y);
        }
    }
}
