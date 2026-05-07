package com.raytracer.render;

import com.raytracer.Ray;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@link DepthOfFieldStrategy} jitters each primary ray's <i>origin</i> across a
 * lens aperture and aims every sample at a single focus point on the focal plane —
 * the inverse of the pinhole construction. These tests pin the focal-plane miss
 * branch, the aim-at-focus invariant, and the lens-vs-pixel jitter footprint
 * independently of the shading model via a recording {@link PathIntegrator} fake.
 *
 * <p>Geometry convention: the focal {@code Plane} is built with normal {@code (0,0,-1)}
 * and distance {@code dofFocalDist}, which places the plane at world {@code z = dofFocalDist}.
 * The ray origin is the pixel point at {@code z = scrZ} and its direction has
 * {@code dz < 0}, so the ray hits the plane only when {@code dofFocalDist < scrZ}.
 */
class DepthOfFieldStrategyTest {

    private static final class RecordingIntegrator implements PathIntegrator {
        final List<double[]> origins = new ArrayList<>();
        final List<double[]> dirs    = new ArrayList<>();

        @Override
        public void trace(Ray ray, int depth, double rindex,
                          double[] outColour, boolean inside, int rayNum) {
            origins.add(ray.point.clone());
            dirs.add(ray.direct.clone());
            outColour[0] = 0.5; outColour[1] = 0.5; outColour[2] = 0.5;
        }
    }

    private static final double[] EYE   = {0.0, 0.0, 5.0};
    private static final double   SCR_Z = 0.0;

    /**
     * With {@code dofFocalDist = -3.0 < scrZ = 0}, every eye-through-pixel ray hits the
     * focal plane and the strategy traces all gridSize lens samples.
     */
    @Test
    void focalPlaneHitTracesGridSizeRays() {
        RecordingIntegrator integrator = new RecordingIntegrator();
        ThreadLocalRandomSource rng = new ThreadLocalRandomSource();
        rng.reseed(11);
        DepthOfFieldStrategy s = new DepthOfFieldStrategy(
                EYE, SCR_Z, 0.4, 0.4, -3.0, integrator, rng);

        double[] rgb = new double[3];
        s.shadePixel(0.0, 0.0, 1.0, 1.0, 4, 4, rgb);

        assertEquals(16, integrator.origins.size(), "4x4 grid must fire 16 lens rays");
        assertArrayEquals(new double[]{0.5, 0.5, 0.5}, rgb, 1e-12,
                "average of constant integrator output must equal that constant");
    }

    /**
     * With {@code dofFocalDist = 5.0 ≥ scrZ = 0}, the focal plane sits behind (or at) the
     * pixel relative to the ray direction, so the eye-through-pixel ray cannot reach it;
     * the strategy must skip the per-pixel ray loop, leave the integrator untouched, and
     * emit black.
     */
    @Test
    void focalPlaneMissShortCircuitsAndEmitsBlack() {
        RecordingIntegrator integrator = new RecordingIntegrator();
        ThreadLocalRandomSource rng = new ThreadLocalRandomSource();
        rng.reseed(11);
        DepthOfFieldStrategy s = new DepthOfFieldStrategy(
                EYE, SCR_Z, 0.4, 0.4, 5.0, integrator, rng);

        double[] rgb = {7.0, 8.0, 9.0};
        s.shadePixel(0.0, 0.0, 1.0, 1.0, 4, 4, rgb);

        assertEquals(0, integrator.origins.size(),
                "focal-plane miss must not invoke the integrator");
        assertArrayEquals(new double[]{0.0, 0.0, 0.0}, rgb, 0.0,
                "miss must emit literal (0,0,0) so packArgb yields 0xFF000000");
    }

    /**
     * The aperture is centred on the pixel at {@code z = scrZ}, so every lens origin
     * must lie on the lens plane and inside the aperture rectangle.
     */
    @Test
    void lensOriginsLieInsideApertureCenteredOnPixel() {
        RecordingIntegrator integrator = new RecordingIntegrator();
        ThreadLocalRandomSource rng = new ThreadLocalRandomSource();
        rng.reseed(2026);
        double pixelX = 1.0, pixelY = -1.0;
        double apertureW = 0.4, apertureH = 0.6;
        DepthOfFieldStrategy s = new DepthOfFieldStrategy(
                EYE, SCR_Z, apertureW, apertureH, -3.0, integrator, rng);

        double[] rgb = new double[3];
        s.shadePixel(pixelX, pixelY, 1.0, 1.0, 4, 4, rgb);

        assertEquals(16, integrator.origins.size());
        for (double[] origin : integrator.origins) {
            assertEquals(SCR_Z, origin[2], 1e-12,
                    "lens origin must lie on the screen plane (z = scrZ)");
            assertTrue(origin[0] >= pixelX - apertureW / 2.0
                    && origin[0] <= pixelX + apertureW / 2.0,
                    "lens x out of aperture: " + origin[0]);
            assertTrue(origin[1] >= pixelY - apertureH / 2.0
                    && origin[1] <= pixelY + apertureH / 2.0,
                    "lens y out of aperture: " + origin[1]);
        }
    }

    /**
     * Every lens sample aims at the same focus point — the intersection of the
     * eye-through-pixel ray with the focal plane. So all rays from one pixel converge
     * at exactly one world-space point on that plane.
     */
    @Test
    void allLensRaysAimAtTheSameFocusPoint() {
        RecordingIntegrator integrator = new RecordingIntegrator();
        ThreadLocalRandomSource rng = new ThreadLocalRandomSource();
        rng.reseed(2026);
        double dofFocalDist = -3.0;          // focal plane at z = -3 (in front of screen)
        double pixelX = 0.5, pixelY = 0.25;
        DepthOfFieldStrategy s = new DepthOfFieldStrategy(
                EYE, SCR_Z, 0.4, 0.4, dofFocalDist, integrator, rng);

        double[] rgb = new double[3];
        s.shadePixel(pixelX, pixelY, 1.0, 1.0, 4, 4, rgb);

        assertEquals(16, integrator.origins.size(), "test relies on a hit");

        // All rays must pass through one world-space point on the focal plane (z = dofFocalDist).
        // Walk each ray to z = dofFocalDist and assert (x, y) match within epsilon.
        Double fx = null, fy = null;
        for (int i = 0; i < integrator.origins.size(); i++) {
            double[] o = integrator.origins.get(i);
            double[] d = integrator.dirs.get(i);
            double tFocus = (dofFocalDist - o[2]) / d[2];
            double rxAt = o[0] + tFocus * d[0];
            double ryAt = o[1] + tFocus * d[1];
            if (fx == null) { fx = rxAt; fy = ryAt; }
            assertEquals(fx, rxAt, 1e-9, "ray " + i + " misses focus x");
            assertEquals(fy, ryAt, 1e-9, "ray " + i + " misses focus y");
        }
    }
}
