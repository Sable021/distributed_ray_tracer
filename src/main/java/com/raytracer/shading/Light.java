package com.raytracer.shading;

import com.raytracer.render.RandomSource;
import com.raytracer.render.Sampler;

/**
 * Light source contract. Both point lights (single shadow ray) and area lights (multiple
 * stratified shadow rays) implement this interface; the shader treats them uniformly,
 * dividing the accumulated colour by {@link #sampleCount} to average area-light samples
 * (point lights produce 1 sample, so {@code /1.0} is a bit-exact no-op).
 */
public sealed interface Light permits PointLight, AreaLight {

    /** Diffuse emission RGB. */
    double[] diffuseEmission();

    /** Specular emission RGB. */
    double[] specularEmission();

    /**
     * Number of shadow samples to take for this light.
     * @param areaSubSamples renderer-configured area-light sub-sample count (ignored by
     *                       point lights, which always return 1)
     */
    int sampleCount(int areaSubSamples);

    /**
     * Fill {@code out} with the world-space position of the {@code k}-th shadow sample.
     * Stratification uses {@code rayNum} (via {@code sampler}) so adjacent primary rays
     * sample different cells; jitter within a cell uses {@code rng}. Point lights ignore
     * both injectables.
     */
    void samplePosition(int k, int rayNum, RandomSource rng, Sampler sampler, double[] out);

    /**
     * Exclusive upper bound for the shadow-caster scan when shadow-testing this light.
     * Point lights return 15 (preserved C++ quirk where the original code hardcoded
     * {@code j<15}); area lights return {@code sceneSize}.
     */
    int shadowCasterScanLimit(int sceneSize);
}
