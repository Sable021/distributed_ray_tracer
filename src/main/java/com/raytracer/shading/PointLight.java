package com.raytracer.shading;

import com.raytracer.VecMath;
import com.raytracer.render.RandomSource;
import com.raytracer.render.Sampler;

/**
 * Single-position light source. Casts exactly one shadow ray per shaded sample.
 *
 * <p>Returns 15 from {@link #shadowCasterScanLimit} regardless of scene size — the C++
 * original hardcoded {@code j<15} for point-light shadow occluder scans, and Phase B
 * preserves that quirk verbatim.
 */
public final class PointLight implements Light {

    private static final int CPP_SHADOW_CASTER_LIMIT = 15;

    private final double[] position;
    private final double[] diffuseEmission;
    private final double[] specularEmission;

    public PointLight(double[] position, double[] diffuse, double[] specular) {
        this.position         = position.clone();
        this.diffuseEmission  = diffuse.clone();
        this.specularEmission = specular.clone();
    }

    @Override public double[] diffuseEmission()  { return diffuseEmission; }
    @Override public double[] specularEmission() { return specularEmission; }
    @Override public int      sampleCount(int areaSubSamples) { return 1; }

    @Override
    public void samplePosition(int k, int rayNum, RandomSource rng, Sampler sampler, double[] out) {
        VecMath.copy(position, out);
    }

    @Override
    public int shadowCasterScanLimit(int sceneSize) {
        return CPP_SHADOW_CASTER_LIMIT;
    }
}
