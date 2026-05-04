package com.raytracer;

import com.raytracer.geom.Primitive;
import com.raytracer.shading.Material;

/**
 * A scene element: geometric {@link Primitive} plus either a {@link Material} (shaded
 * surface) or {@code null} material (light emitter — a corresponding {@link
 * com.raytracer.shading.Light} is registered on {@link Scene#lights}).
 *
 * <p>The pre-Phase-B kitchen-sink struct (26 mutable fields including {@code vectors[7][3]},
 * Phong coefficients, area-light grid storage and a {@code String} texture key) is gone:
 * material state is now an immutable {@link Material} record and light state lives on the
 * {@code Light} subclass it belongs to.
 */
public class SceneObject {

    /** Geometric body. */
    public Primitive primitive;

    /** Surface properties; {@code null} marks an emitter. */
    public Material material;

    /** If true, this object is skipped when tracing primary (depth=1) rays. */
    public boolean skipPrimaryRays;

    /** True if this object emits light. Lights are skipped as shaded surfaces. */
    public boolean isLight() { return material == null; }
}
