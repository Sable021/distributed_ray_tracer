package com.raytracer;

import static com.raytracer.SceneObject.ObjectType.*;

/** Recursive shader. One instance per render; holds the scene and max-depth config. */
public final class RayTracer {

    private static final double LARGE_FLOAT = 99_999_999.0;

    /** Global ambient added to every shaded point — matches C++ global_amb. */
    private static final double[] GLOBAL_AMB = { 0.05, 0.05, 0.05 };

    /** Area-light shadow sampling: 4 sub-samples per shade call, fixed by C++ behaviour. */
    private static final int AREA_LIGHT_SUB_SAMPLES = 4;

    private final Scene scene;
    private final int maxDepth;
    private final int gridX, gridY;   // area-light stratification grid (matches supersample grid)

    public RayTracer(Scene scene, int maxDepth, int gridX, int gridY) {
        this.scene = scene;
        this.maxDepth = maxDepth;
        this.gridX = gridX;
        this.gridY = gridY;
    }

    // -------------------------------------------------------------------------
    // Intersection dispatch
    // -------------------------------------------------------------------------

    /** Returns the nearest hit object index (writing the point into outIntersect), or -1. */
    public int intersectScene(Ray ray, double[] outIntersect, int depth) {
        int objectIndex = -1;
        double nearestT = LARGE_FLOAT;
        double[] cand = new double[3];

        for (int i = 0; i < scene.numActive; i++) {
            SceneObject obj = scene.objects[i];
            if (obj.type == UNASSIGNED) continue;

            // Primary rays: skip the front area light so it doesn't eclipse the scene
            if (depth == 1 && i == Scene.SKIP_AT_DEPTH_1) continue;

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

    /** Returns t, or -1.0 on miss. Applies the bounded-quad test for indices 15/16. */
    public double intersectObject(Ray ray, int index) {
        SceneObject obj = scene.objects[index];
        if (obj.type == UNASSIGNED) return -1.0;

        return switch (obj.type) {
            case SPHERE   -> Intersect.raySphereIntersect(ray, obj);
            case TRIANGLE -> Intersect.rayTriIntersect(ray, obj);
            case PLANE    -> {
                double tp = Intersect.rayPlaneIntersect(ray, obj);
                if (tp > 0.0 && Scene.isBoundedQuad(index)) {
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

    public void rayTrace(Ray ray, int depth, double rindex,
                         double[] outColour, boolean inside, int rayNum) {
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
        double[] V = { -ray.direct[0], -ray.direct[1], -ray.direct[2] };

        shadeObject(idx, intersect, V, localColour, rayNum);

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
                rayTrace(refractRay, depth + 1, objRIndex, refractColour, !inside, rayNum);
            } else {
                // Total internal reflection — bounce back inside the object
                Intersect.reflection(ray.direct, N, refrDir);
                Ray refractRay = Ray.make(intersect, refrDir);
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
            if (obj.glossiness <= 0.0 || depth > 4) {
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
            rayTrace(reflectRay, depth + 1, rindex, reflectColour, inside, rayNum);
        }

        outColour[0] = localColour[0] + obj.refl * reflectColour[0] + obj.refr * refractColour[0];
        outColour[1] = localColour[1] + obj.refl * reflectColour[1] + obj.refr * refractColour[1];
        outColour[2] = localColour[2] + obj.refl * reflectColour[2] + obj.refr * refractColour[2];
    }

    // -------------------------------------------------------------------------
    // Phong shading (point + area lights)
    // -------------------------------------------------------------------------

    private void shadeObject(int index, double[] intersect, double[] V,
                             double[] outColour, int rayNum) {
        double[] N = new double[3];
        double[] L = new double[3];
        double[] R = new double[3];
        double[] diffColour   = new double[3];
        double[] specColour   = new double[3];
        double[] objectColour = new double[3];

        scene.getObjectColour(index, intersect, objectColour);

        SceneObject self = scene.objects[index];
        int gridSize = gridX * gridY;

        for (int i = 0; i < scene.numActive; i++) {
            SceneObject light = scene.objects[i];
            if (!light.isLight) continue;

            VecMath.set(diffColour, 0, 0, 0);
            VecMath.set(specColour, 0, 0, 0);

            if (light.type == SPHERE) {
                // ---- Point light ----
                VecMath.direction(L, intersect, light.vectors[0]);
                VecMath.normalize(L);
                Ray shadowRay = Ray.make(intersect, L);

                double shadow = 1.0;
                // C++ hardcodes j<15 — only test non-light, non-plane objects as shadow casters.
                for (int j = 0; j < 15; j++) {
                    if (scene.objects[j].type == PLANE) continue;
                    if (!scene.objects[j].isLight && intersectObject(shadowRay, j) > 0) {
                        if (scene.objects[j].refr > 0) {
                            shadow *= 0.6;
                        } else {
                            shadow = 0;
                            break;
                        }
                    }
                }

                if (shadow > 0) {
                    Intersect.getNormal(self, intersect, N);
                    double NdotL = VecMath.dot(N, L);
                    if (NdotL > 0) {
                        double kd = self.diffuse;
                        diffColour[0] = shadow * kd * NdotL * objectColour[0] * light.vectors[1][0];
                        diffColour[1] = shadow * kd * NdotL * objectColour[1] * light.vectors[1][1];
                        diffColour[2] = shadow * kd * NdotL * objectColour[2] * light.vectors[1][2];

                        if (self.specular_r > 0) {
                            R[0] = 2 * NdotL * N[0] - L[0];
                            R[1] = 2 * NdotL * N[1] - L[1];
                            R[2] = 2 * NdotL * N[2] - L[2];
                            VecMath.normalize(R);

                            double VdotR = Math.max(0.0, VecMath.dot(V, R));  // clamp (Math.pow NaN fix)
                            double ks = self.specular_r;
                            double pow = Math.pow(VdotR, self.n);

                            specColour[0] = shadow * ks * pow * light.vectors[2][0];
                            specColour[1] = shadow * ks * pow * light.vectors[2][1];
                            specColour[2] = shadow * ks * pow * light.vectors[2][2];
                        }
                    }
                }

                outColour[0] += diffColour[0] + specColour[0];
                outColour[1] += diffColour[1] + specColour[1];
                outColour[2] += diffColour[2] + specColour[2];

            } else if (light.type == PLANE) {
                // ---- Area light (4 stratified sub-samples) ----
                double[] sampleVertex = new double[3];
                double[] sampleColour = new double[3];

                Sampling.LightGridDelta lgd = Sampling.getLightGridDelta(light);
                double[] lgdX = lgd.lightGridDX();
                double[] lgdY = lgd.lightGridDY();

                for (int k = 0; k < AREA_LIGHT_SUB_SAMPLES; k++) {
                    VecMath.set(diffColour, 0, 0, 0);
                    VecMath.set(specColour, 0, 0, 0);

                    int[] xy = Sampling.getGridNumber((rayNum + k) % gridSize, gridX, gridY);
                    int gx = xy[0], gy = xy[1];

                    double jx = Rng.uniform(0.0, 1.0);
                    double jy = Rng.uniform(0.0, 1.0);

                    sampleVertex[0] = light.lightGridSample[gy][gx][0] + jx * lgdX[0] + jy * lgdY[0];
                    sampleVertex[1] = light.lightGridSample[gy][gx][1] + jx * lgdX[1] + jy * lgdY[1];
                    sampleVertex[2] = light.lightGridSample[gy][gx][2] + jx * lgdX[2] + jy * lgdY[2];

                    VecMath.direction(L, intersect, sampleVertex);
                    VecMath.normalize(L);
                    Ray shadowRay = Ray.make(intersect, L);

                    double shadow = 1.0;
                    for (int j = 0; j < scene.numActive; j++) {
                        if (scene.objects[j].type == PLANE || scene.objects[j].isLight) continue;
                        if (!scene.objects[j].isLight && intersectObject(shadowRay, j) > 0) {
                            if (scene.objects[j].refr > 0) {
                                shadow *= 0.6;
                            } else {
                                shadow = 0;
                                break;
                            }
                        }
                    }

                    if (shadow > 0) {
                        Intersect.getNormal(self, intersect, N);
                        double NdotL = VecMath.dot(N, L);
                        if (NdotL > 0) {
                            double kd = self.diffuse;
                            diffColour[0] = shadow * kd * NdotL * objectColour[0] * light.vectors[1][0];
                            diffColour[1] = shadow * kd * NdotL * objectColour[1] * light.vectors[1][1];
                            diffColour[2] = shadow * kd * NdotL * objectColour[2] * light.vectors[1][2];

                            if (self.specular_r > 0) {
                                R[0] = 2 * NdotL * N[0] - L[0];
                                R[1] = 2 * NdotL * N[1] - L[1];
                                R[2] = 2 * NdotL * N[2] - L[2];
                                VecMath.normalize(R);

                                double VdotR = Math.max(0.0, VecMath.dot(V, R));
                                double ks = self.specular_r;
                                double pow = Math.pow(VdotR, self.n);

                                specColour[0] = shadow * ks * pow * light.vectors[2][0];
                                specColour[1] = shadow * ks * pow * light.vectors[2][1];
                                specColour[2] = shadow * ks * pow * light.vectors[2][2];
                            }
                        }
                    }

                    sampleColour[0] += diffColour[0] + specColour[0];
                    sampleColour[1] += diffColour[1] + specColour[1];
                    sampleColour[2] += diffColour[2] + specColour[2];
                }

                outColour[0] += sampleColour[0] / AREA_LIGHT_SUB_SAMPLES;
                outColour[1] += sampleColour[1] / AREA_LIGHT_SUB_SAMPLES;
                outColour[2] += sampleColour[2] / AREA_LIGHT_SUB_SAMPLES;
            }
        }

        outColour[0] += GLOBAL_AMB[0];
        outColour[1] += GLOBAL_AMB[1];
        outColour[2] += GLOBAL_AMB[2];
    }
}
