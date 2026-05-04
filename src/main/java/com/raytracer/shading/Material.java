package com.raytracer.shading;

/**
 * Bundle of surface properties consumed by the shader: albedo source ({@link Texture}),
 * direct-lighting model ({@link BRDF}), and the four scalar coefficients controlling
 * recursive ray spawning (reflection, refraction, refractive index, glossiness).
 *
 * <p>Lights have no Material — a {@code null} on {@link com.raytracer.SceneObject#material}
 * marks an emitter.
 */
public record Material(
        Texture albedo,
        BRDF    brdf,
        double  reflectivity,
        double  transmittance,
        double  indexOfRefraction,
        double  glossiness
) {}
