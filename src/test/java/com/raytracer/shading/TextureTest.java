package com.raytracer.shading;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextureTest {

    @Test
    void solidColorReturnsConstant() {
        Texture t = new SolidColorTexture(0.25, 0.5, 0.75);
        double[] out = new double[3];

        t.sample(new double[]{1, 2, 3}, out);
        assertArrayEquals(new double[]{0.25, 0.5, 0.75}, out, 0.0);

        t.sample(new double[]{-100, 0, 5.5}, out);
        assertArrayEquals(new double[]{0.25, 0.5, 0.75}, out, 0.0);
    }

    /**
     * Origin maps to {@code sin(0) = 0} on every axis, which puts the sample on the
     * {@code tp[0] >= 0} branch with all even cells. Locks the boundary handling.
     */
    @Test
    void checkerAtOrigin() {
        Texture t = new CheckerTexture();
        double[] out = new double[3];
        t.sample(new double[]{0, 0, 0}, out);

        // tp = (0,0,0); cx=cy=cz=0 → all even, xyEven=true; tp[0]>=0, zEven → (0.4, 0.4, 0.4)
        assertArrayEquals(new double[]{0.4, 0.4, 0.4}, out, 1e-12);
    }

    /** Stripes light/dark dispatch is driven by {@code (radius+0.5)%5 < 1}. */
    @Test
    void stripesAtOriginPicksDarkBand() {
        Texture t = new StripesTexture();
        double[] out = new double[3];
        t.sample(new double[]{0, 0, 0}, out);

        // tp = (0,0,0) → radius = 0; angle = π/2 (tp[2]==0 branch); radius += 2*sin(20*π/2)
        // = 2*sin(10π) ≈ 0; (0+0.5) → grain = 0 → light band.
        assertEquals(0.82, out[0], 1e-9);   // R_LIGHT
        assertEquals(0.67, out[1], 1e-9);   // G_LIGHT
        assertEquals(0.56, out[2], 1e-9);   // B_LIGHT
    }

    /** Perlin noise is deterministic across runs (seed is hardcoded to 12345L). */
    @Test
    void perlinNoiseIsDeterministic() {
        double[] p = {1.5, 2.5, 3.5};
        double a = PerlinNoise.noise(p);
        double b = PerlinNoise.noise(p);
        assertEquals(a, b, 0.0);
    }

    @Test
    void perlinNoiseRangeIsRoughlyBounded() {
        // Output should fall in roughly [-1, 1] for any input.
        for (int i = 0; i < 100; i++) {
            double[] p = {i * 0.37, i * 1.13 - 5, i * -0.77 + 2};
            double v = PerlinNoise.noise(p);
            assertTrue(v > -1.5 && v < 1.5, "noise out of range at " + i + ": " + v);
        }
    }
}
