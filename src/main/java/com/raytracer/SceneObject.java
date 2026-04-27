package com.raytracer;

/**
 * Mutable data holder describing a single primitive in the scene: its geometry, material,
 * and (if applicable) light-source attributes.
 *
 * <p>Mirrors the C++ {@code Object} struct one-for-one, using public mutable fields for
 * direct member access in tight loops. The geometry-specific data lives in {@link #vectors}
 * with a per-{@link ObjectType} layout described below.
 */
public class SceneObject {

    /** Geometry kind. {@code UNASSIGNED} entries are skipped during intersection. */
    public enum ObjectType { UNASSIGNED, SPHERE, PLANE, TRIANGLE }

    public ObjectType type = ObjectType.UNASSIGNED;

    /**
     * Geometry data, indexed [slot][xyz]. Slot meaning depends on {@link #type}:
     * <ul>
     *   <li><b>Plane:</b>    {@code [0]} = unit normal</li>
     *   <li><b>Sphere:</b>   {@code [0]} = centre</li>
     *   <li><b>Triangle:</b> {@code [0..2]} = vertices (anticlockwise winding), {@code [3]} = unit normal</li>
     *   <li><b>Light:</b>    {@code [0]} = centre/normal, {@code [1]} = diffuse colour,
     *       {@code [2]} = specular colour, {@code [3..6]} = corners (area light only)</li>
     * </ul>
     */
    public double[][] vectors = new double[7][3];

    /** Plane only: signed distance from the origin along the normal. Unused for other types. */
    public double dist;
    /** Sphere only: radius in world units. */
    public double radius;

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
    /** Pre-computed grid of sample points across an area light's surface. Filled by {@link Sampling#createLightGrid}. */
    public double[][][] lightGridSample = new double[Sampling.MAX_GRID_Y][Sampling.MAX_GRID_X][3];
    /** Step vector along the light's local X axis between adjacent grid samples. */
    public double[]     lightGridDX     = new double[3];
    /** Step vector along the light's local Y axis between adjacent grid samples. */
    public double[]     lightGridDY     = new double[3];
    /** Step magnitude along the light's local X axis (jitter range). */
    public double       lightDX;
    /** Step magnitude along the light's local Y axis (jitter range). */
    public double       lightDY;
}
