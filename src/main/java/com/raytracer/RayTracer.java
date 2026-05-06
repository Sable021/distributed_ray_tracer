package com.raytracer;

import com.raytracer.geom.Plane;
import com.raytracer.render.Accelerator;
import com.raytracer.render.RandomSource;
import com.raytracer.render.Sampler;
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
    private final Accelerator accelerator;
    private final RandomSource rng;
    private final Sampler sampler;

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
     * @param scene       pre-initialised scene to render
     * @param maxDepth    maximum recursion depth for reflected/refracted rays
     * @param gridX       supersample/area-light stratification grid width
     * @param gridY       supersample/area-light stratification grid height
     * @param config      algorithm constants (ambient level, shadow samples, etc.)
     * @param accelerator scene query backend (already {@link Accelerator#build} bound to {@code scene})
     * @param rng         random source shared with the {@link Renderer} so per-row reseeding remains deterministic
     * @param sampler     stratification policy for glossy + area-light grids
     */
    public RayTracer(Scene scene, int maxDepth, int gridX, int gridY, RenderConfig config,
                     Accelerator accelerator, RandomSource rng, Sampler sampler) {
        this.scene = scene;
        this.maxDepth = maxDepth;
        this.gridX = gridX;
        this.gridY = gridY;
        this.config = config;
        this.accelerator = accelerator;
        this.rng = rng;
        this.sampler = sampler;
    }

    // -------------------------------------------------------------------------
    // Intersection dispatch
    // -------------------------------------------------------------------------

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
     *   <li>Find the nearest hit object via the injected {@link Accelerator}.</li>
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
        int idx = accelerator.nearest(ray, depth, intersect);

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
                glossyGridSample(sampleVertex, gridX, gridY, reflDir, gridMidpt, rayNum);

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
                light.samplePosition(k, rayNum, rng, sampler, samplePos);
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

    /** sqrt(0.125) — diagonal offset for the glossy reflection sample grid. */
    private static final double GLOSSY_DIAG_HALF = Math.sqrt(0.125);

    /**
     * Compute one jittered sample point on the glossy reflection grid (a square grid
     * orthogonal to the perfect-reflection direction, centred at {@code midpt}).
     * Uses the injected {@link Sampler} for the deterministic cell pick and the injected
     * {@link RandomSource} for sub-cell jitter — both contributing to the per-row hash
     * stability across runs.
     */
    private void glossyGridSample(double[] outVertex, int gsizeX, int gsizeY,
                                  double[] N, double[] midpt, int traceNum) {
        double DX = 0.5 / gsizeX;
        double DY = 0.5 / gsizeY;

        double[] V = {0.5, 0.5, 0.5};
        double[] dia1 = new double[3];
        double[] dia2 = new double[3];
        VecMath.cross(dia1, V, N);
        VecMath.cross(dia2, dia1, N);

        double[] gridOrigin = new double[3];
        VecMath.pointOnLine(gridOrigin, midpt, dia1, GLOSSY_DIAG_HALF);

        double[] gridXAxis = new double[3];
        VecMath.direction(gridXAxis, dia1, dia2);
        VecMath.normalize(gridXAxis);

        double[] gridYAxis = new double[3];
        VecMath.cross(gridYAxis, gridXAxis, N);

        gridXAxis[0] *= DX; gridXAxis[1] *= DX; gridXAxis[2] *= DX;
        gridYAxis[0] *= DY; gridYAxis[1] *= DY; gridYAxis[2] *= DY;

        double rx = rng.uniform(0.0, 1.0);
        double ry = rng.uniform(0.0, 1.0);

        int[] xy = sampler.cellForRay(traceNum, gsizeX, gsizeY);
        int x = xy[0], y = xy[1];

        outVertex[0] = gridOrigin[0] + (x+rx)*gridXAxis[0] + (y+ry)*gridYAxis[0];
        outVertex[1] = gridOrigin[1] + (x+rx)*gridXAxis[1] + (y+ry)*gridYAxis[1];
        outVertex[2] = gridOrigin[2] + (x+rx)*gridXAxis[2] + (y+ry)*gridYAxis[2];
    }
}
