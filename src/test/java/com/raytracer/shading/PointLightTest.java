package com.raytracer.shading;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PointLightTest {

    /**
     * The C++ original hardcodes {@code j<15} for point-light shadow occluder scans.
     * Phase B preserves this quirk verbatim — {@link PointLight} owns it as the only
     * place the magic 15 lives.
     */
    @Test
    void shadowCasterScanLimitIs15ForAnySceneSize() {
        PointLight pl = new PointLight(
                new double[]{0, 5, 0},
                new double[]{1, 1, 1},
                new double[]{1, 1, 1});

        assertEquals(15, pl.shadowCasterScanLimit(3));
        assertEquals(15, pl.shadowCasterScanLimit(17));
        assertEquals(15, pl.shadowCasterScanLimit(99));
    }

    @Test
    void sampleCountIsAlwaysOne() {
        PointLight pl = new PointLight(
                new double[]{0, 5, 0},
                new double[]{1, 1, 1},
                new double[]{1, 1, 1});

        assertEquals(1, pl.sampleCount(4));
        assertEquals(1, pl.sampleCount(64));
    }

    @Test
    void samplePositionReturnsTheLightPosition() {
        double[] pos = {1.5, -2.0, 7.25};
        PointLight pl = new PointLight(pos,
                new double[]{1, 1, 1},
                new double[]{1, 1, 1});
        double[] out = new double[3];

        pl.samplePosition(0, 42, out);

        assertArrayEquals(pos, out, 0.0);
    }

    @Test
    void positionAndEmissionAreDefensivelyCopied() {
        double[] pos       = {1, 2, 3};
        double[] diff      = {0.4, 0.5, 0.6};
        double[] spec      = {0.7, 0.8, 0.9};
        PointLight pl = new PointLight(pos, diff, spec);

        pos[0]  = 999;
        diff[0] = 999;
        spec[0] = 999;

        double[] out = new double[3];
        pl.samplePosition(0, 0, out);
        assertEquals(1, out[0]);
        assertEquals(0.4, pl.diffuseEmission()[0]);
        assertEquals(0.7, pl.specularEmission()[0]);
    }
}
