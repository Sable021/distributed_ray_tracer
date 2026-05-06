package com.raytracer;

import com.raytracer.geom.BoundedQuad;
import com.raytracer.geom.Plane;
import com.raytracer.geom.Sphere;
import com.raytracer.geom.Triangle;
import com.raytracer.shading.AreaLight;
import com.raytracer.shading.CheckerTexture;
import com.raytracer.shading.Light;
import com.raytracer.shading.Material;
import com.raytracer.shading.PhongBRDF;
import com.raytracer.shading.SolidColorTexture;
import com.raytracer.shading.StripesTexture;
import com.raytracer.shading.Texture;

import java.util.ArrayList;
import java.util.List;

/**
 * Hardcoded scene description. Mirrors the C++ {@code initialise_scene()} layout exactly so
 * material and geometry tuning (including the C++ quirks now encapsulated in each
 * {@link com.raytracer.geom.Primitive Primitive} impl) keeps producing the reference image.
 *
 * <p>Construction is via the static factory {@link #initialise()}, which builds and returns
 * a fully-populated immutable {@code Scene}. Per-object data lives on {@link SceneObject},
 * indexed 0..16 as documented inside the factory; light emitters are also collected into
 * {@link #lights}.
 */
public final class Scene {

    /** Backing array length. Matches the C++ {@code new Object[20]} allocation. */
    public static final int SIZE        = 20;
    /** Number of populated entries; objects at indices 0..16 are valid scene primitives. */
    public static final int NUM_ACTIVE  = 17;

    /** All scene primitives (length {@link #SIZE}). Use {@link #numActive} to bound iteration. */
    public final SceneObject[] objects;
    /** Number of valid entries in {@link #objects}; equals {@link #NUM_ACTIVE}. */
    public final int numActive;
    /** All emitters in the scene; both point and area lights. Iteration order is shading order. */
    public final List<Light> lights;

    public Scene(SceneObject[] objects, int numActive, List<Light> lights) {
        this.objects   = objects;
        this.numActive = numActive;
        this.lights    = lights;
    }

    /**
     * Build the canonical scene: floor + 4 walls/ceiling, 4 spheres (one mirror, two stacked
     * solids, one refractive glass), a coloured tetrahedron, and 2 area lights (a ceiling
     * panel and an off-screen back-wall light for indirect illumination).
     */
    public static Scene initialise() {
        SceneObject[] o = new SceneObject[SIZE];
        for (int i = 0; i < SIZE; i++) o[i] = new SceneObject();

        Texture checker = new CheckerTexture();
        Texture stripes = new StripesTexture();

        // ---- Walls and floor/ceiling ----
        o[0].primitive = new Plane(new double[]{0.0, 1.0, 0.0}, 0.0);
        o[0].material  = new Material(checker, new PhongBRDF(0.8, 0.0, 0), 0.3, 0.0, 0.0, 0.0);

        o[1].primitive = new Plane(new double[]{1.0, 0.0, 0.0}, 9.0);
        o[1].material  = new Material(solid(0.7, 0.7, 0.4), new PhongBRDF(0.5, 0.0, 0), 0.0, 0.0, 0.0, 0.0);

        o[2].primitive = new Plane(new double[]{-1.0, 0.0, 0.0}, 9.0);
        o[2].material  = new Material(solid(0.7, 0.7, 0.4), new PhongBRDF(0.5, 0.0, 0), 0.0, 0.0, 0.0, 0.0);

        o[3].primitive = new Plane(new double[]{0.0, 0.0, 1.0}, 5.0);
        o[3].material  = new Material(solid(0.2, 0.6, 0.6), new PhongBRDF(0.75, 0.0, 0), 0.2, 0.0, 0.0, 0.0);

        o[4].primitive = new Plane(new double[]{0.0, -1.0, 0.0}, 10.0);
        o[4].material  = new Material(solid(0.2, 0.6, 0.6), new PhongBRDF(0.7, 0.4, 20), 0.4, 0.0, 0.0, 0.0);

        // ---- Spheres ----
        o[5].primitive = new Sphere(new double[]{-3.0, 3.0, -1.5}, 2.25);
        o[5].material  = new Material(solid(0.15, 0.15, 0.15), new PhongBRDF(1.0, 1.0, 100), 1.0, 0.0, 0.0, 5.0);

        o[6].primitive = new Sphere(new double[]{4.2, 2.0, 1.0}, 1.5);
        o[6].material  = new Material(solid(1.0, 0.0, 0.0), new PhongBRDF(0.8, 0.75, 40), 0.5, 0.0, 0.0, 0.8);

        o[7].primitive = new Sphere(new double[]{4.0, 6.2, 1.0}, 1.5);
        o[7].material  = new Material(solid(0.9, 0.85, 0.0), new PhongBRDF(0.8, 0.5, 20), 0.3, 0.0, 0.0, 2.1);

        o[8].primitive = new Sphere(new double[]{0.3, 2.0, 3.8}, 1.3);
        o[8].material  = new Material(solid(0.1, 0.1, 0.1), new PhongBRDF(1.0, 0.85, 60), 0.15, 0.95, 1.5, 0.0);

        // ---- Tetrahedron (4 triangles) ----
        o[9].primitive = new Triangle(
                new double[]{-6.3, 0.0, 2.5},
                new double[]{-3.5, 0.0, 4.5},
                new double[]{-3.0, 0.0, 1.5},
                new double[]{ 0.0, -1.0, 0.0});
        o[9].material  = new Material(stripes, new PhongBRDF(1.0, 0.0, 0), 0.0, 0.0, 0.0, 0.0);

        o[10].primitive = new Triangle(
                new double[]{-6.3, 0.0, 2.5},
                new double[]{-3.5, 0.0, 4.5},
                new double[]{-3.8, 3.5, 3.0},
                new double[]{-0.556759, 0.287154, 0.779463});
        o[10].material = new Material(stripes, new PhongBRDF(1.0, 0.0, 0), 0.0, 0.0, 0.0, 0.0);

        o[11].primitive = new Triangle(
                new double[]{-3.5, 0.0, 4.5},
                new double[]{-3.0, 0.0, 1.5},
                new double[]{-3.8, 3.5, 3.0},
                new double[]{ 0.984405, 0.063464, 0.164068});
        o[11].material = new Material(stripes, new PhongBRDF(1.0, 0.0, 0), 0.0, 0.0, 0.0, 0.0);

        o[12].primitive = new Triangle(
                new double[]{-3.0, 0.0, 1.5},
                new double[]{-6.3, 0.0, 2.5},
                new double[]{-3.8, 3.5, 3.0},
                new double[]{-0.274163, 0.326011, -0.904738});
        o[12].material = new Material(stripes, new PhongBRDF(1.0, 0.0, 0), 0.0, 0.0, 0.0, 0.0);

        // ---- Area lights ----
        List<Light> lights = new ArrayList<>();

        // Ceiling-mounted area light
        double[][] cornersCeil = {
            { 1.0, 9.99, 0.5 },
            { 3.0, 9.99, 0.5 },
            { 3.0, 9.99, 2.5 },
            { 1.0, 9.99, 2.5 }
        };
        AreaLight ceilingLight = new AreaLight(
                new double[]{1.0, 1.0, 1.0},
                new double[]{1.0, 1.0, 1.0},
                cornersCeil);
        o[15].primitive = new BoundedQuad(
                new double[]{0.0, -1.0, 0.0}, 9.99,
                cornersCeil[0], cornersCeil[2]);
        o[15].material  = null;
        lights.add(ceilingLight);

        // Back-wall-facing area light (used only for indirect illumination)
        double[][] cornersBack = {
            { -1.0, 4.0, 6.0 },
            { -3.0, 4.0, 6.0 },
            { -3.0, 6.0, 6.0 },
            { -1.0, 6.0, 6.0 }
        };
        AreaLight backLight = new AreaLight(
                new double[]{1.0, 1.0, 1.0},
                new double[]{1.0, 1.0, 1.0},
                cornersBack);
        o[16].primitive = new BoundedQuad(
                new double[]{0.0, 0.0, -1.0}, 6.0,
                cornersBack[0], cornersBack[2]);
        o[16].material  = null;
        o[16].skipPrimaryRays = true;
        lights.add(backLight);

        return new Scene(o, NUM_ACTIVE, lights);
    }

    private static SolidColorTexture solid(double r, double g, double b) {
        return new SolidColorTexture(r, g, b);
    }

    /** Compute the surface colour at the intersection point by sampling the object's material albedo. */
    public void getObjectColour(int idx, double[] intersect, double[] outColour) {
        objects[idx].material.albedo().sample(intersect, outColour);
    }
}
