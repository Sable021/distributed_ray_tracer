package com.raytracer;

import static com.raytracer.SceneObject.ObjectType.*;

/**
 * Hardcoded scene description. Mirrors the C++ {@code initialise_scene()} layout exactly so
 * material and geometry tuning (including the quirks baked into the intersection math) keep
 * producing the reference image.
 *
 * <p>Construction is via the static factory {@link #initialise()}, which builds and returns
 * a fully-populated immutable {@code Scene}. Per-object data lives on {@link SceneObject},
 * indexed 0..16 as documented inside the factory.
 */
public final class Scene {

    /** Backing array length. Matches the C++ {@code new Object[20]} allocation. */
    public static final int SIZE        = 20;
    /** Number of populated entries; objects at indices 0..16 are valid scene primitives. */
    public static final int NUM_ACTIVE  = 17;

    /**
     * Primary rays skip object 16 (the back-wall area light). Without this the light would
     * eclipse the visible scene because it sits between the eye and the back wall.
     */
    public static final int SKIP_AT_DEPTH_1 = 16;

    /** All scene primitives (length {@link #SIZE}). Use {@link #numActive} to bound iteration. */
    public final SceneObject[] objects;
    /** Number of valid entries in {@link #objects}; equals {@link #NUM_ACTIVE}. */
    public final int numActive;

    Scene(SceneObject[] objects, int numActive) {
        this.objects = objects;
        this.numActive = numActive;
    }

    /** Returns true if the object at {@code idx} is an area-light bounded quad. */
    public boolean isBoundedQuad(int idx) {
        return objects[idx].isLight && objects[idx].type == SceneObject.ObjectType.PLANE;
    }

    /**
     * Build the canonical scene: floor + 4 walls/ceiling, 4 spheres (one mirror, two stacked
     * solids, one refractive glass), a coloured tetrahedron, and 2 area lights (a ceiling
     * panel and an off-screen back-wall light for indirect illumination).
     */
    public static Scene initialise() {
        SceneObject[] o = new SceneObject[SIZE];
        for (int i = 0; i < SIZE; i++) o[i] = new SceneObject();

        // ---- Walls and floor/ceiling ----

        // Floor
        o[0].type = PLANE;
        VecMath.set(o[0].vectors[0], 0.0, 1.0, 0.0);
        o[0].dist = 0.0;
        VecMath.set(o[0].colour, 0.4, 0.4, 0.4);
        o[0].diffuse = 0.8;
        o[0].refl = 0.3;

        // Left wall
        o[1].type = PLANE;
        VecMath.set(o[1].vectors[0], 1.0, 0.0, 0.0);
        o[1].dist = 9.0;
        VecMath.set(o[1].colour, 0.7, 0.7, 0.4);
        o[1].diffuse = 0.5;

        // Right wall
        o[2].type = PLANE;
        VecMath.set(o[2].vectors[0], -1.0, 0.0, 0.0);
        o[2].dist = 9.0;
        VecMath.set(o[2].colour, 0.7, 0.7, 0.4);
        o[2].diffuse = 0.5;

        // Back wall
        o[3].type = PLANE;
        VecMath.set(o[3].vectors[0], 0.0, 0.0, 1.0);
        o[3].dist = 5.0;
        VecMath.set(o[3].colour, 0.2, 0.6, 0.6);
        o[3].diffuse = 0.75;
        o[3].refl = 0.2;

        // Ceiling
        o[4].type = PLANE;
        VecMath.set(o[4].vectors[0], 0.0, -1.0, 0.0);
        o[4].dist = 10.0;
        VecMath.set(o[4].colour, 0.2, 0.6, 0.6);
        o[4].diffuse = 0.7;
        o[4].specular_r = 0.4;
        o[4].n = 20;
        o[4].refl = 0.4;

        // ---- Spheres ----

        // Mirror sphere (glossy)
        o[5].type = SPHERE;
        VecMath.set(o[5].vectors[0], -3.0, 3.0, -1.5);
        o[5].radius = 2.25;
        VecMath.set(o[5].colour, 0.15, 0.15, 0.15);
        o[5].diffuse = 1.0;
        o[5].specular_r = 1.0;
        o[5].n = 100;
        o[5].glossiness = 5.0;
        o[5].refl = 1.0;

        // Red sphere (stacked bottom)
        o[6].type = SPHERE;
        VecMath.set(o[6].vectors[0], 4.2, 2.0, 1.0);
        o[6].radius = 1.5;
        VecMath.set(o[6].colour, 1.0, 0.0, 0.0);
        o[6].diffuse = 0.8;
        o[6].specular_r = 0.75;
        o[6].n = 40;
        o[6].glossiness = 0.8;
        o[6].refl = 0.5;

        // Yellow sphere (stacked top)
        o[7].type = SPHERE;
        VecMath.set(o[7].vectors[0], 4.0, 6.2, 1.0);
        o[7].radius = 1.5;
        VecMath.set(o[7].colour, 0.9, 0.85, 0.0);
        o[7].diffuse = 0.8;
        o[7].specular_r = 0.5;
        o[7].n = 20;
        o[7].glossiness = 2.1;
        o[7].refl = 0.3;

        // Translucent sphere
        o[8].type = SPHERE;
        VecMath.set(o[8].vectors[0], 0.3, 2.0, 3.8);
        o[8].radius = 1.3;
        VecMath.set(o[8].colour, 0.1, 0.1, 0.1);
        o[8].rindex = 1.5;
        o[8].diffuse = 1.0;
        o[8].specular_r = 0.85;
        o[8].specular_t = 0.8;
        o[8].n = 60;
        o[8].refl = 0.15;
        o[8].refr = 0.95;

        // ---- Tetrahedron (4 triangles) ----

        // Bottom face
        o[9].type = TRIANGLE;
        VecMath.set(o[9].vectors[0], -6.3, 0.0, 2.5);
        VecMath.set(o[9].vectors[1], -3.5, 0.0, 4.5);
        VecMath.set(o[9].vectors[2], -3.0, 0.0, 1.5);
        VecMath.set(o[9].vectors[3],  0.0, -1.0, 0.0);
        VecMath.set(o[9].colour, 1.0, 1.0, 0.0);
        o[9].diffuse = 1.0;

        // Left face
        o[10].type = TRIANGLE;
        VecMath.set(o[10].vectors[0], -6.3, 0.0, 2.5);
        VecMath.set(o[10].vectors[1], -3.5, 0.0, 4.5);
        VecMath.set(o[10].vectors[2], -3.8, 3.5, 3.0);
        VecMath.set(o[10].vectors[3], -0.556759, 0.287154, 0.779463);
        VecMath.set(o[10].colour, 0.0, 1.0, 0.0);
        o[10].diffuse = 1.0;

        // Right face
        o[11].type = TRIANGLE;
        VecMath.set(o[11].vectors[0], -3.5, 0.0, 4.5);
        VecMath.set(o[11].vectors[1], -3.0, 0.0, 1.5);
        VecMath.set(o[11].vectors[2], -3.8, 3.5, 3.0);
        VecMath.set(o[11].vectors[3],  0.984405, 0.063464, 0.164068);
        VecMath.set(o[11].colour, 0.0, 0.0, 1.0);
        o[11].diffuse = 1.0;

        // Back face
        o[12].type = TRIANGLE;
        VecMath.set(o[12].vectors[0], -3.0, 0.0, 1.5);
        VecMath.set(o[12].vectors[1], -6.3, 0.0, 2.5);
        VecMath.set(o[12].vectors[2], -3.8, 3.5, 3.0);
        VecMath.set(o[12].vectors[3], -0.274163, 0.326011, -0.904738);
        VecMath.set(o[12].colour, 1.0, 0.0, 0.0);
        o[12].diffuse = 1.0;

        // ---- Area light 1 (ceiling-mounted) ----
        o[15].type = PLANE;
        VecMath.set(o[15].vectors[0], 0.0, -1.0, 0.0);
        VecMath.set(o[15].vectors[1], 1.0, 1.0, 1.0);   // diffuse
        VecMath.set(o[15].vectors[2], 1.0, 1.0, 1.0);   // specular
        VecMath.set(o[15].vectors[3], 1.0, 9.99, 0.5);  // corner
        VecMath.set(o[15].vectors[4], 3.0, 9.99, 0.5);
        VecMath.set(o[15].vectors[5], 3.0, 9.99, 2.5);  // opposite corner
        VecMath.set(o[15].vectors[6], 1.0, 9.99, 2.5);
        o[15].dist = 9.99;
        VecMath.set(o[15].colour, 1.0, 1.0, 1.0);
        o[15].isLight = true;

        // ---- Area light 2 (back-wall-facing, used only for indirect illumination) ----
        o[16].type = PLANE;
        VecMath.set(o[16].vectors[0], 0.0, 0.0, -1.0);
        VecMath.set(o[16].vectors[1], 1.0, 1.0, 1.0);
        VecMath.set(o[16].vectors[2], 1.0, 1.0, 1.0);
        VecMath.set(o[16].vectors[3], -1.0, 4.0, 6.0);
        VecMath.set(o[16].vectors[4], -3.0, 4.0, 6.0);
        VecMath.set(o[16].vectors[5], -3.0, 6.0, 6.0);
        VecMath.set(o[16].vectors[6], -1.0, 6.0, 6.0);
        o[16].dist = 6.0;
        VecMath.set(o[16].colour, 1.0, 1.0, 1.0);
        o[16].isLight = true;

        // Procedural textures
        o[0].texture = "checkerboard";
        for (int i = 9; i <= 12; i++) o[i].texture = "stripes";

        // Back-wall area light is skipped by primary rays (would eclipse the scene)
        o[16].skipPrimaryRays = true;

        return new Scene(o, NUM_ACTIVE);
    }

    /**
     * Compute the surface colour at the intersection point, dispatching to texture functions
     * based on the object's {@link SceneObject#texture} field; falls back to the base colour.
     */
    public void getObjectColour(int idx, double[] intersect, double[] outColour) {
        String tex = objects[idx].texture;
        if ("checkerboard".equals(tex)) {
            Textures.mixChecks(intersect, outColour);
        } else if ("stripes".equals(tex)) {
            Textures.strips(intersect, outColour);
        } else {
            VecMath.copy(objects[idx].colour, outColour);
        }
    }

}
