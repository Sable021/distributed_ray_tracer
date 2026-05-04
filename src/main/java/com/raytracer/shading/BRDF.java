package com.raytracer.shading;

/**
 * Bidirectional reflectance distribution function. Implementations compute one light's
 * diffuse + specular contribution at a shaded point given the surface frame and the
 * (already-attenuated) incoming illumination.
 *
 * <p>{@link #shade} <b>accumulates</b> into {@code outRgb} — it does not zero the buffer.
 * Callers manage initialisation, summing across multiple lights, and area-light averaging.
 */
public interface BRDF {

    /**
     * Add this light's contribution to {@code outRgb}.
     *
     * @param albedo    surface albedo at the hit point (sampled from {@link Material#albedo()})
     * @param normal    surface normal at the hit point (unit length)
     * @param view      direction from hit point toward the eye (unit length)
     * @param lightDir  direction from hit point toward the light sample (unit length)
     * @param lightDiff diffuse emission RGB of the light
     * @param lightSpec specular emission RGB of the light
     * @param shadow    visibility scalar in [0,1]: 1 = unshadowed, 0 = fully blocked
     * @param outRgb    accumulator (caller-owned, not zeroed)
     */
    void shade(double[] albedo, double[] normal, double[] view,
               double[] lightDir, double[] lightDiff, double[] lightSpec,
               double shadow, double[] outRgb);
}
