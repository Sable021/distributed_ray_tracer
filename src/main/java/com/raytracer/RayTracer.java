package com.raytracer;

import com.raytracer.geom.Plane;
import com.raytracer.shading.Light;
import com.raytracer.shading.Material;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Recursive Phong shader with reflection, refraction, total-internal-reflection, glossy
 * reflection, and area-light soft shadows.
 *
 * <p>Public entry point is {@link #rayTrace}, which is invoked once per primary ray per
 * pixel sample by {@link Renderer}. {@code rayTrace} dispatches to {@link #intersectScene}
 * to find the nearest hit, then to {@link #shadeObject} (Phong) plus recursive
 * {@code rayTrace} calls along reflected/refracted directions until {@code maxDepth} is
 * reached.
 *
 * <p>One instance per render; immutable after construction (scene and configuration are
 * fixed).
 */
public final class RayTracer {

    private final Scene scene;
    private final int maxDepth;
    private final int gridX, gridY;   // area-light stratification grid (matches supersample grid)
    private final RenderConfig config;

    // Ray counters — incremented from every render thread, snapshot via getRayCounts()
    private final AtomicLong primaryRays  = new AtomicLong();
    private final AtomicLong shadowRays   = new AtomicLong();
    private final AtomicLong reflectRays  = new AtomicLong();
    private final AtomicLong refractRays  = new AtomicLong();

    /**
     * Snapshot of rays cast so far, broken down by type. Counts are monotonic — they only
     * grow during a render and are not reset between renders on the same {@link RayTracer}.
     */
    public record RayCounts(long primary, long shadow, long reflect, long refract) {
        public long total() { return primary + shadow + reflect + refract; }
    }

    /** Thread-safe snapshot of the current ray counts. */
    public RayCounts getRayCounts() {
        return new RayCounts(primaryRays.get(), shadowRays.get(),
                             reflectRays.get(), refractRays.get());
    }

    /**
     * @param scene    pre-initialised scene to render
     * @param maxDepth maximum recursion depth for reflected/refracted rays
     * @param gridX    supersample/area-light stratification grid width
     * @param gridY    supersample/area-light stratification grid height
     * @param config   algorithm constants (ambient level, shadow samples, etc.)
     */
    public RayTracer(Scene scene, int maxDepth, int gridX, int gridY, RenderConfig config) {
        this.scene = scene;
        this.maxDepth = maxDepth;
        this.gridX = gridX;
        this.gridY = gridY;
        this.config = config;
    }

    // -------------------------------------------------------------------------
    // Intersection dispatch
    // -------------------------------------------------------------------------

    /**
     * Find the nearest object hit by {@code ray}, writing the intersection point into
     * {@code outIntersect}. Returns the object index, or -1 if the ray misses everything.
     *
     * <p>{@code depth == 1} (primary rays from the camera) skips objects whose
     * {@link SceneObject#skipPrimaryRays} flag is set so the front-facing area light
     * doesn't eclipse the scene; secondary rays still see it.
     */
    public int intersectScene(Ray ray, double[] outIntersect, int depth) {
        int objectIndex = -1;
        double nearestT = Double.POSITIVE_INFINITY;
        double[] cand = new double[3];

        for (int i = 0; i < scene.numActive; i++) {
            SceneObject obj = scene.objects[i];
            if (obj.primitive == null) continue;

            // Primary rays: skip objects flagged to avoid eclipsing the scene
            if (depth == 1 && obj.skipPrimaryRays) continue;

            double t = obj.primitive.intersect(ray);
            if (t > 0.0 && t < nearestT) {
                VecMath.pointOnLine(cand, ray.point, ray.direct, t);
                VecMath.copy(cand, outIntersect);
                objectIndex = i;
                nearestT = t;
            }
        }
        return objectIndex;
    }

    /** Test {@code ray} against a single scene object by index. -1.0 on miss. */
    public double intersectObject(Ray ray, int index) {
        SceneObject obj = scene.objects[index];
        if (obj.primitive == null) return -1.0;
        return obj.primitive.intersect(ray);
    }

    // -------------------------------------------------------------------------
    // Recursive shading
    // -------------------------------------------------------------------------

    /**
     * Recursively shade the colour seen along {@code ray}.
     *
     * <p>Algorithm at a glance:
     * <ol>
     *   <li>Find the nearest hit object via {@link #intersectScene}.</li>
     *   <li>If it's a light, return white. If it misses, return black.</li>
     *   <li>Compute the local Phong shading via {@link #shadeObject}.</li>
     *   <li>If the surface is refractive and depth allows, recurse along the
     *       refracted direction (handling total internal reflection).</li>
     *   <li>If the surface is reflective (and we're not inside a medium), recurse
     *       along the (possibly glossy-jittered) reflected direction.</li>
     *   <li>Combine local + reflection*kr + refraction*kt into {@code outColour}.</li>
     * </ol>
     *
     * @param ray       ray to trace
     * @param depth     current recursion depth (primary rays start at 1)
     * @param rindex    refractive index of the medium the ray is currently inside
     * @param outColour caller-owned RGB[3] receiving the shaded colour
     * @param inside    true if the ray is travelling inside a refractive object
     * @param rayNum    sample index used to deterministically pick a stratified
     *                  area-light/glossy sub-cell
     */
    public void rayTrace(Ray ray, int depth, double rindex,
                         double[] outColour, boolean inside, int rayNum) {
        if (depth == 1) primaryRays.incrementAndGet();

        if (depth > maxDepth) {
            VecMath.set(outColour, 0, 0, 0);
            return;
        }

        double[] intersect = new double[3];
        int idx = intersectScene(ray, intersect, depth);

        if (idx == -1) {
            VecMath.set(outColour, 0, 0, 0);
            return;
        }
        SceneObject obj = scene.objects[idx];
        if (obj.isLight()) {
            VecMath.set(outColour, 1.0, 1.0, 1.0);
            return;
        }

        Material mat = obj.material;

        double[] localColour   = new double[3];
        double[] reflectColour = new double[3];
        double[] refractColour = new double[3];
        double[] N = new double[3];
        double[] view = { -ray.direct[0], -ray.direct[1], -ray.direct[2] };

        shadeObject(idx, intersect, view, localColour, rayNum);

        // ---- Refraction ----
        if (mat.transmittance() > 0.0 && depth != maxDepth) {
            double objRIndex = mat.indexOfRefraction();
            obj.primitive.normalAt(intersect, N);

            if (inside) {
                N[0] = -N[0]; N[1] = -N[1]; N[2] = -N[2];
                objRIndex = 1.0;  // exiting into air
            }

            double[] refrDir = new double[3];
            boolean refracted = Intersect.refraction(ray.direct, N, rindex, objRIndex, refrDir);

            if (refracted) {
                Ray refractRay = Ray.make(intersect, refrDir);
                refractRays.incrementAndGet();
                rayTrace(refractRay, depth + 1, objRIndex, refractColour, !inside, rayNum);
            } else {
                // Total internal reflection — bounce back inside the object
                Intersect.reflection(ray.direct, N, refrDir);
                Ray refractRay = Ray.make(intersect, refrDir);
                refractRays.incrementAndGet();
                rayTrace(refractRay, depth + 1, rindex, refractColour, !inside, rayNum);
            }
        }

        // ---- Reflection (outside-only) ----
        if (mat.reflectivity() > 0.0 && !inside && depth != maxDepth) {
            obj.primitive.normalAt(intersect, N);
            double[] reflDir = new double[3];
            Intersect.reflection(ray.direct, N, reflDir);
            VecMath.normalize(reflDir);

            Ray reflectRay;
            if (mat.glossiness() <= 0.0 || depth > config.glossyMaxDepth()) {
                reflectRay = Ray.make(intersect, reflDir);
            } else {
                // Glossy: sample a jittered direction from a grid orthogonal to the perfect reflection
                double[] gridMidpt = new double[3];
                VecMath.pointOnLine(gridMidpt, intersect, reflDir, mat.glossiness());
                double[] sampleVertex = new double[3];
                Sampling.getSampleVertex(sampleVertex, gridX, gridY, reflDir, gridMidpt, rayNum);

                double[] sampleRefl = new double[3];
                VecMath.direction(sampleRefl, intersect, sampleVertex);
                VecMath.normalize(sampleRefl);

                reflectRay = Ray.make(intersect, sampleRefl);
            }
            reflectRays.incrementAndGet();
            rayTrace(reflectRay, depth + 1, rindex, reflectColour, inside, rayNum);
        }

        outColour[0] = localColour[0] + mat.reflectivity() * reflectColour[0] + mat.transmittance() * refractColour[0];
        outColour[1] = localColour[1] + mat.reflectivity() * reflectColour[1] + mat.transmittance() * refractColour[1];
        outColour[2] = localColour[2] + mat.reflectivity() * reflectColour[2] + mat.transmittance() * refractColour[2];
    }

    // -------------------------------------------------------------------------
    // Phong shading (point + area lights, unified loop)
    // -------------------------------------------------------------------------

    /**
     * Compute Phong-shaded local colour at {@code intersect} on object {@code index}.
     *
     * <p>Iterates {@link Scene#lights}; each light yields {@link Light#sampleCount} samples
     * (1 for {@link com.raytracer.shading.PointLight}, {@link RenderConfig#areaLightSubSamples}
     * for {@link com.raytracer.shading.AreaLight}). Per-sample contributions are summed and
     * divided by {@code sampleCount} before being added to {@code outColour}; for point
     * lights {@code n=1} so the divide is a bit-exact identity. A small global ambient term
     * is added at the end.
     */
    private void shadeObject(int index, double[] intersect, double[] view,
                             double[] outColour, int rayNum) {
        SceneObject self = scene.objects[index];
        Material    mat  = self.material;

        double[] objectColour = new double[3];
        scene.getObjectColour(index, intersect, objectColour);

        double[] N = new double[3];
        self.primitive.normalAt(intersect, N);
        VecMath.normalize(N);

        double[] L         = new double[3];
        double[] samplePos = new double[3];
        int areaSubSamples = config.areaLightSubSamples();

        for (Light light : scene.lights) {
            int n = light.sampleCount(areaSubSamples);
            int scanLimit = light.shadowCasterScanLimit(scene.numActive);

            double[] sampleColour = new double[3];

            for (int k = 0; k < n; k++) {
                light.samplePosition(k, rayNum, samplePos);
                VecMath.direction(L, intersect, samplePos);
                VecMath.normalize(L);

                double shadow = shadowVisibility(intersect, L, scanLimit);
                if (shadow <= 0.0) continue;

                mat.brdf().shade(objectColour, N, view, L,
                                 light.diffuseEmission(), light.specularEmission(),
                                 shadow, sampleColour);
            }

            outColour[0] += sampleColour[0] / n;
            outColour[1] += sampleColour[1] / n;
            outColour[2] += sampleColour[2] / n;
        }

        outColour[0] += config.globalAmb()[0];
        outColour[1] += config.globalAmb()[1];
        outColour[2] += config.globalAmb()[2];
    }

    /**
     * Cast one shadow ray from {@code intersect} along {@code L} and return the visibility
     * scalar. Opaque occluders return {@code 0}; refractive occluders attenuate by
     * {@link RenderConfig#refractiveShadowAttenuation} per hit. Walls (Plane) and lights
     * are never treated as shadow casters.
     *
     * @param scanLimit exclusive upper bound for the scan; point lights pass 15
     *                  (preserved C++ quirk), area lights pass {@link Scene#numActive}
     */
    private double shadowVisibility(double[] intersect, double[] L, int scanLimit) {
        Ray shadowRay = Ray.make(intersect, L);
        shadowRays.incrementAndGet();

        double shadow = 1.0;
        for (int j = 0; j < scanLimit; j++) {
            SceneObject blocker = scene.objects[j];
            if (blocker.primitive instanceof Plane || blocker.isLight()) continue;
            if (intersectObject(shadowRay, j) > 0) {
                if (blocker.material != null && blocker.material.transmittance() > 0) {
                    shadow *= config.refractiveShadowAttenuation();
                } else {
                    return 0.0;
                }
            }
        }
        return shadow;
    }
}
