package com.raytracer;

import java.util.concurrent.atomic.AtomicLong;

import static com.raytracer.SceneObject.ObjectType.*;

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
            if (obj.type == UNASSIGNED) continue;

            // Primary rays: skip objects flagged to avoid eclipsing the scene
            if (depth == 1 && scene.objects[i].skipPrimaryRays) continue;

            double t = intersectObject(ray, i);
            if (t > 0.0 && t < nearestT) {
                VecMath.pointOnLine(cand, ray.point, ray.direct, t);
                VecMath.copy(cand, outIntersect);
                objectIndex = i;
                nearestT = t;
            }
        }
        return objectIndex;
    }

    /**
     * Test {@code ray} against a single scene object by index, dispatching on its type.
     * Indices 15/16 are area-light "bounded quads" — infinite planes additionally clipped
     * to a rectangular region, so a plane hit is rejected if it falls outside the corner
     * extents stored on the object.
     *
     * @return parametric distance along the ray, or -1.0 on miss
     */
    public double intersectObject(Ray ray, int index) {
        SceneObject obj = scene.objects[index];
        if (obj.type == UNASSIGNED) return -1.0;

        return switch (obj.type) {
            case SPHERE    -> Intersect.raySphereIntersect(ray, obj);
            case TRIANGLE  -> Intersect.rayTriIntersect(ray, obj);
            case CYLINDER  -> Intersect.rayCylinderIntersect(ray, obj);
            case PLANE    -> {
                double tp = Intersect.rayPlaneIntersect(ray, obj);
                if (tp > 0.0 && scene.isBoundedQuad(index)) {
                    double[] pt = new double[3];
                    VecMath.pointOnLine(pt, ray.point, ray.direct, tp);

                    double[] normal = obj.vectors[0];
                    double[] v3 = obj.vectors[3];
                    double[] v5 = obj.vectors[5];

                    // Along each axis where the plane extends (normal component is 0),
                    // the intersection must lie between the corner values of vectors[3] and vectors[5].
                    for (int k = 0; k < 3; k++) {
                        if (normal[k] == 0.0) {
                            double lo = Math.min(v3[k], v5[k]);
                            double hi = Math.max(v3[k], v5[k]);
                            if (pt[k] < lo || pt[k] > hi) {
                                tp = -1.0;
                                break;
                            }
                        }
                    }
                }
                yield tp;
            }
            default -> -1.0;
        };
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
        if (scene.objects[idx].isLight) {
            VecMath.set(outColour, 1.0, 1.0, 1.0);
            return;
        }

        SceneObject obj = scene.objects[idx];

        double[] localColour   = new double[3];
        double[] reflectColour = new double[3];
        double[] refractColour = new double[3];
        double[] N = new double[3];
        double[] view = { -ray.direct[0], -ray.direct[1], -ray.direct[2] };

        shadeObject(idx, intersect, view, localColour, rayNum);

        // ---- Refraction ----
        if (obj.refr > 0.0 && depth != maxDepth) {
            double objRIndex = obj.rindex;
            Intersect.getNormal(obj, intersect, N);

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
        if (obj.refl > 0.0 && !inside && depth != maxDepth) {
            Intersect.getNormal(obj, intersect, N);
            double[] reflDir = new double[3];
            Intersect.reflection(ray.direct, N, reflDir);
            VecMath.normalize(reflDir);

            Ray reflectRay;
            if (obj.glossiness <= 0.0 || depth > config.glossyMaxDepth()) {
                reflectRay = Ray.make(intersect, reflDir);
            } else {
                // Glossy: sample a jittered direction from a grid orthogonal to the perfect reflection
                double[] gridMidpt = new double[3];
                VecMath.pointOnLine(gridMidpt, intersect, reflDir, obj.glossiness);
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

        outColour[0] = localColour[0] + obj.refl * reflectColour[0] + obj.refr * refractColour[0];
        outColour[1] = localColour[1] + obj.refl * reflectColour[1] + obj.refr * refractColour[1];
        outColour[2] = localColour[2] + obj.refl * reflectColour[2] + obj.refr * refractColour[2];
    }

    // -------------------------------------------------------------------------
    // Phong shading (point + area lights)
    // -------------------------------------------------------------------------

    /**
     * Compute Phong-shaded local colour at {@code intersect} on object {@code index}.
     *
     * <p>Iterates over every light in the scene. Point lights ({@code type == SPHERE})
     * emit a single shadow ray. Area lights ({@code type == PLANE}) take
     * {@link #AREA_LIGHT_SUB_SAMPLES} stratified jittered samples across the light's
     * surface for soft shadows. Both contribute diffuse + specular terms; opaque
     * occluders fully shadow, refractive occluders attenuate by 0.6 per hit. A small
     * {@link #GLOBAL_AMB} ambient term is always added.
     */
    private void shadeObject(int index, double[] intersect, double[] view,
                             double[] outColour, int rayNum) {
        double[] L            = new double[3];
        double[] diffColour   = new double[3];
        double[] specColour   = new double[3];
        double[] objectColour = new double[3];

        scene.getObjectColour(index, intersect, objectColour);

        int gridSize = gridX * gridY;

        for (int i = 0; i < scene.numActive; i++) {
            SceneObject light = scene.objects[i];
            if (!light.isLight) continue;

            if (light.type == SPHERE) {
                // ---- Point light: single shadow ray ----
                VecMath.direction(L, intersect, light.vectors[0]);
                VecMath.normalize(L);

                // C++ hardcodes the shadow-caster bound to j<15; preserved for parity.
                accumulateLightContribution(index, i, intersect, view, L, objectColour,
                                            15, diffColour, specColour);

                outColour[0] += diffColour[0] + specColour[0];
                outColour[1] += diffColour[1] + specColour[1];
                outColour[2] += diffColour[2] + specColour[2];

            } else if (light.type == PLANE) {
                // ---- Area light: average AREA_LIGHT_SUB_SAMPLES jittered shadow rays ----
                double[] sampleVertex = new double[3];
                double[] sampleColour = new double[3];

                Sampling.LightGridDelta lgd = Sampling.getLightGridDelta(light);
                double[] lgdX = lgd.lightGridDX();
                double[] lgdY = lgd.lightGridDY();

                for (int k = 0; k < config.areaLightSubSamples(); k++) {
                    int[] xy = Sampling.getGridNumber((rayNum + k) % gridSize, gridX, gridY);
                    int gx = xy[0], gy = xy[1];

                    double jx = Rng.uniform(0.0, 1.0);
                    double jy = Rng.uniform(0.0, 1.0);

                    sampleVertex[0] = light.lightGridSample[gy][gx][0] + jx * lgdX[0] + jy * lgdY[0];
                    sampleVertex[1] = light.lightGridSample[gy][gx][1] + jx * lgdX[1] + jy * lgdY[1];
                    sampleVertex[2] = light.lightGridSample[gy][gx][2] + jx * lgdX[2] + jy * lgdY[2];

                    VecMath.direction(L, intersect, sampleVertex);
                    VecMath.normalize(L);

                    accumulateLightContribution(index, i, intersect, view, L, objectColour,
                                                scene.numActive, diffColour, specColour);

                    sampleColour[0] += diffColour[0] + specColour[0];
                    sampleColour[1] += diffColour[1] + specColour[1];
                    sampleColour[2] += diffColour[2] + specColour[2];
                }

                outColour[0] += sampleColour[0] / config.areaLightSubSamples();
                outColour[1] += sampleColour[1] / config.areaLightSubSamples();
                outColour[2] += sampleColour[2] / config.areaLightSubSamples();
            }
        }

        outColour[0] += config.globalAmb()[0];
        outColour[1] += config.globalAmb()[1];
        outColour[2] += config.globalAmb()[2];
    }

    /**
     * Cast one shadow ray from {@code intersect} along {@code L} toward a (point or
     * area-light sub-sample) light, then write the resulting Phong diffuse and specular
     * contributions into {@code outDiff} / {@code outSpec}. Both outputs are zeroed at
     * entry, so callers can sum them directly without pre-clearing.
     *
     * <p>Opaque occluders zero the contribution; refractive occluders attenuate by
     * {@code 0.6} per hit. Planes and lights are never treated as shadow casters.
     *
     * <p>Note the {@code Math.max(0, view·R)} clamp before {@code Math.pow}: Java's
     * {@code Math.pow} returns {@code NaN} for a negative base with a non-integer
     * exponent, which would produce black-pixel artifacts at glancing-angle highlights.
     * The C++ original got away without the clamp because C's {@code pow} returns 0.
     *
     * @param shadowCasterEnd exclusive upper bound for the shadow-caster scan; point
     *                        lights pass 15 (C++ quirk), area lights pass numActive
     */
    private void accumulateLightContribution(int selfIdx, int lightIdx,
                                             double[] intersect, double[] view, double[] L,
                                             double[] objectColour, int shadowCasterEnd,
                                             double[] outDiff, double[] outSpec) {
        VecMath.set(outDiff, 0, 0, 0);
        VecMath.set(outSpec, 0, 0, 0);

        Ray shadowRay = Ray.make(intersect, L);
        shadowRays.incrementAndGet();

        double shadow = 1.0;
        for (int j = 0; j < shadowCasterEnd; j++) {
            SceneObject blocker = scene.objects[j];
            if (blocker.type == PLANE || blocker.isLight) continue;
            if (intersectObject(shadowRay, j) > 0) {
                if (blocker.refr > 0) {
                    shadow *= config.refractiveShadowAttenuation();
                } else {
                    shadow = 0;
                    break;
                }
            }
        }

        if (shadow <= 0) return;

        SceneObject self  = scene.objects[selfIdx];
        SceneObject light = scene.objects[lightIdx];

        double[] N = new double[3];
        Intersect.getNormal(self, intersect, N);
        double NdotL = VecMath.dot(N, L);
        if (NdotL <= 0) return;

        double kd = self.diffuse;
        outDiff[0] = shadow * kd * NdotL * objectColour[0] * light.vectors[1][0];
        outDiff[1] = shadow * kd * NdotL * objectColour[1] * light.vectors[1][1];
        outDiff[2] = shadow * kd * NdotL * objectColour[2] * light.vectors[1][2];

        if (self.specular_r > 0) {
            double[] R = {
                2 * NdotL * N[0] - L[0],
                2 * NdotL * N[1] - L[1],
                2 * NdotL * N[2] - L[2]
            };
            VecMath.normalize(R);

            double VdotR = Math.max(0.0, VecMath.dot(view, R));  // clamp (Math.pow NaN fix)
            double ks = self.specular_r;
            double pow = Math.pow(VdotR, self.n);

            outSpec[0] = shadow * ks * pow * light.vectors[2][0];
            outSpec[1] = shadow * ks * pow * light.vectors[2][1];
            outSpec[2] = shadow * ks * pow * light.vectors[2][2];
        }
    }
}
