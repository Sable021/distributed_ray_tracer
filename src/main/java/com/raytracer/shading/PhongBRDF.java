package com.raytracer.shading;

import com.raytracer.VecMath;

/**
 * Classic Phong reflectance: diffuse term {@code kd · (N·L)} plus specular term
 * {@code ks · (V·R)^n}, weighted by light emission and shadow visibility.
 *
 * <p>{@code Math.max(0, V·R)} is clamped before {@link Math#pow} because Java returns
 * {@code NaN} for a negative base with a non-integer exponent — black-pixel artifacts
 * at glancing-angle highlights. The C++ original got away without the clamp because
 * C's {@code pow} returns 0.
 */
public final class PhongBRDF implements BRDF {

    private final double kd;
    private final double ks;
    private final int    n;

    /**
     * @param kd diffuse coefficient
     * @param ks specular-reflection coefficient (may be 0 to disable the highlight)
     * @param n  shininess exponent (higher = tighter highlight)
     */
    public PhongBRDF(double kd, double ks, int n) {
        this.kd = kd;
        this.ks = ks;
        this.n  = n;
    }

    @Override
    public void shade(double[] albedo, double[] normal, double[] view,
                      double[] lightDir, double[] lightDiff, double[] lightSpec,
                      double shadow, double[] outRgb) {
        double NdotL = VecMath.dot(normal, lightDir);
        if (NdotL <= 0.0) return;

        double diffR = shadow * kd * NdotL * albedo[0] * lightDiff[0];
        double diffG = shadow * kd * NdotL * albedo[1] * lightDiff[1];
        double diffB = shadow * kd * NdotL * albedo[2] * lightDiff[2];

        double specR = 0.0, specG = 0.0, specB = 0.0;
        if (ks > 0.0) {
            double[] R = {
                2 * NdotL * normal[0] - lightDir[0],
                2 * NdotL * normal[1] - lightDir[1],
                2 * NdotL * normal[2] - lightDir[2]
            };
            VecMath.normalize(R);

            double VdotR = Math.max(0.0, VecMath.dot(view, R));
            double pow   = Math.pow(VdotR, n);

            specR = shadow * ks * pow * lightSpec[0];
            specG = shadow * ks * pow * lightSpec[1];
            specB = shadow * ks * pow * lightSpec[2];
        }

        outRgb[0] += diffR + specR;
        outRgb[1] += diffG + specG;
        outRgb[2] += diffB + specB;
    }
}
