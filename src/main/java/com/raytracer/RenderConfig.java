package com.raytracer;

/** Immutable algorithm constants for a render. Defaults match the original C++ behaviour. */
public record RenderConfig(
    double[] globalAmb,
    int areaLightSubSamples,
    double refractiveShadowAttenuation,
    int glossyMaxDepth,
    boolean acesTonemap
) {
    /** Returns a copy with the area-light sub-sample count replaced. */
    public RenderConfig withShadowSamples(int n) {
        return new RenderConfig(globalAmb, n, refractiveShadowAttenuation, glossyMaxDepth, acesTonemap);
    }

    /** Returns a copy with ACES tone mapping toggled. */
    public RenderConfig withAcesTonemap(boolean enabled) {
        return new RenderConfig(globalAmb, areaLightSubSamples, refractiveShadowAttenuation, glossyMaxDepth, enabled);
    }

    /** Default constants matching the original C++ renderer. */
    public static RenderConfig defaults() {
        return new RenderConfig(new double[]{0.05, 0.05, 0.05}, 4, 0.6, 4, false);
    }
}
