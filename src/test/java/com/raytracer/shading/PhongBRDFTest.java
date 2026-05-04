package com.raytracer.shading;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhongBRDFTest {

    /** Light directly above a unit-up normal: NdotL = 1, kd = 0.5, no specular. */
    @Test
    void diffuseOnly_directOverhead() {
        PhongBRDF brdf = new PhongBRDF(0.5, 0.0, 0);
        double[] albedo    = {1.0, 0.5, 0.25};
        double[] normal    = {0, 1, 0};
        double[] view      = {0, 0, 1};
        double[] lightDir  = {0, 1, 0};
        double[] lightDiff = {1.0, 1.0, 1.0};
        double[] lightSpec = {1.0, 1.0, 1.0};
        double[] out       = new double[3];

        brdf.shade(albedo, normal, view, lightDir, lightDiff, lightSpec, 1.0, out);

        // diff = shadow * kd * NdotL * albedo[i] * lightDiff[i] = 0.5 * albedo
        assertEquals(0.5,    out[0], 1e-12);
        assertEquals(0.25,   out[1], 1e-12);
        assertEquals(0.125,  out[2], 1e-12);
    }

    @Test
    void specularContributesWhenViewAlignsWithReflection() {
        PhongBRDF brdf = new PhongBRDF(0.0, 1.0, 1);  // specular only, n=1
        double[] albedo    = {1.0, 1.0, 1.0};
        double[] normal    = {0, 1, 0};
        double[] view      = {0, 1, 0};   // looking along the normal
        double[] lightDir  = {0, 1, 0};   // light along the normal — R == L mirrored, V·R = 1
        double[] lightDiff = {0, 0, 0};
        double[] lightSpec = {1.0, 1.0, 1.0};
        double[] out       = new double[3];

        brdf.shade(albedo, normal, view, lightDir, lightDiff, lightSpec, 1.0, out);

        // V·R = 1, pow(1, 1) = 1, so spec = ks = 1.0 per channel
        assertEquals(1.0, out[0], 1e-12);
        assertEquals(1.0, out[1], 1e-12);
        assertEquals(1.0, out[2], 1e-12);
    }

    /** NdotL <= 0: light is below the surface, no contribution. */
    @Test
    void backFacingLightContributesNothing() {
        PhongBRDF brdf = new PhongBRDF(1.0, 1.0, 50);
        double[] out = {0.1, 0.2, 0.3};   // pre-loaded; should be untouched

        brdf.shade(
                new double[]{1, 1, 1},
                new double[]{0, 1, 0},      // normal up
                new double[]{0, 0, 1},
                new double[]{0, -1, 0},     // light below surface
                new double[]{1, 1, 1},
                new double[]{1, 1, 1},
                1.0, out);

        assertArrayEquals(new double[]{0.1, 0.2, 0.3}, out, 1e-12);
    }

    /** The clamp must keep V·R >= 0 to avoid {@link Math#pow} returning NaN. */
    @Test
    void glancingViewPowerNotNaN() {
        PhongBRDF brdf = new PhongBRDF(0.0, 1.0, 50);  // n=50, non-integer-friendly
        // View pointing AWAY from reflection so V·R < 0 — without the clamp, pow() returns NaN.
        double[] out = new double[3];
        brdf.shade(
                new double[]{1, 1, 1},
                new double[]{0, 1, 0},
                new double[]{0, -1, 0},     // view facing down — opposite to reflection
                new double[]{0, 1, 0},      // light up
                new double[]{0, 0, 0},
                new double[]{1, 1, 1},
                1.0, out);

        // With clamp: VdotR = max(0, -1) = 0; pow(0, 50) = 0 — no NaN, no contribution.
        assertEquals(0.0, out[0], 1e-12);
        assertFalse(Double.isNaN(out[0]));
    }

    /** Output is accumulated, not replaced. */
    @Test
    void shadeAccumulatesIntoExistingOutput() {
        PhongBRDF brdf = new PhongBRDF(1.0, 0.0, 0);
        double[] out = {0.1, 0.2, 0.3};

        brdf.shade(
                new double[]{1, 0, 0},
                new double[]{0, 1, 0},
                new double[]{0, 0, 1},
                new double[]{0, 1, 0},
                new double[]{1, 1, 1},
                new double[]{1, 1, 1},
                1.0, out);

        // diff for R channel = 1*1*1*1 = 1.0; G/B albedo = 0
        assertEquals(1.1, out[0], 1e-12);
        assertEquals(0.2, out[1], 1e-12);
        assertEquals(0.3, out[2], 1e-12);
    }

    @Test
    void shadowZeroReturnsNothingButZeroIsBitExactNoOp() {
        PhongBRDF brdf = new PhongBRDF(1.0, 1.0, 50);
        double[] out = new double[3];

        brdf.shade(
                new double[]{1, 1, 1},
                new double[]{0, 1, 0},
                new double[]{0, 0, 1},
                new double[]{0, 1, 0},
                new double[]{1, 1, 1},
                new double[]{1, 1, 1},
                0.0, out);

        // shadow = 0 → all products are 0
        assertEquals(0.0, out[0], 0.0);
        assertEquals(0.0, out[1], 0.0);
        assertEquals(0.0, out[2], 0.0);
    }
}
