package com.raytracer;

import com.raytracer.geom.Primitive;

/**
 * Mutable per-scene-element holder bundling a geometric {@link Primitive} with material
 * and (if applicable) light-source attributes.
 *
 * <p>Geometry lives entirely on {@link #primitive}; pre-Phase-A {@code type/dist/radius}
 * fields are gone. Material fields and (for lights) emission slots remain on this struct
 * pending Phase B's {@code Material}/{@code Light} extraction.
 */
public class SceneObject {

    /** Geometric body. {@code null} means "unassigned slot, skip during intersection". */
    public Primitive primitive;

    /**
     * Per-element scratch slots used by light shading (will be replaced by explicit
     * {@code Light} fields in Phase B). Slot meaning when this object is a light:
     * {@code [0]} = position/normal, {@code [1]} = diffuse emission RGB,
     * {@code [2]} = specular emission RGB, {@code [3..6]} = area-light corners.
     */
    public double[][] vectors = new double[7][3];

    // ---- Material parameters ----
    /** Base RGB colour in [0,1]; used directly except where overridden by a procedural texture. */
    public double[] colour     = new double[3];
    /** Reflection coefficient (0 = none, 1 = perfect mirror). */
    public double refl;
    /** Refraction coefficient (0 = opaque, 1 = fully transmissive). */
    public double refr;
    /** Refractive index for refraction (e.g. 1.5 for glass). */
    public double rindex;
    /** Glossy-reflection radius. 0 = perfect mirror; >0 jitters within a sample disk. */
    public double glossiness;
    /** Phong diffuse coefficient (kd). */
    public double diffuse;
    /** Phong specular-reflection coefficient (ks). */
    public double specular_r;
    /** Phong specular-transmission coefficient (used in refraction blending). */
    public double specular_t;
    /** Phong shininess exponent (higher = tighter highlight). */
    public int n;

    // ---- Light source attributes (used only when isLight = true) ----
    /** True if this object emits light. Lights are skipped as shaded surfaces. */
    public boolean isLight;
    /**
     * Pre-computed grid of sample points across an area light's surface, indexed
     * {@code [y][x][component]}. Allocated and filled by {@link Sampling#createLightGrid}
     * to match the configured grid size; remains {@code null} for non-light objects.
     */
    public double[][][] lightGridSample;
    /** Step vector along the light's local X axis between adjacent grid samples. */
    public double[]     lightGridDX     = new double[3];
    /** Step vector along the light's local Y axis between adjacent grid samples. */
    public double[]     lightGridDY     = new double[3];
    /** Step magnitude along the light's local X axis (jitter range). */
    public double       lightDX;
    /** Step magnitude along the light's local Y axis (jitter range). */
    public double       lightDY;

    /** Procedural texture name: {@code "checkerboard"}, {@code "stripes"}, or {@code null}. */
    public String  texture;
    /** If true, this object is skipped when tracing primary (depth=1) rays. */
    public boolean skipPrimaryRays;
}
