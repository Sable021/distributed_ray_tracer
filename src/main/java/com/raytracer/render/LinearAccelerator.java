package com.raytracer.render;

import com.raytracer.Ray;
import com.raytracer.Scene;
import com.raytracer.SceneObject;
import com.raytracer.VecMath;

/**
 * Brute-force {@link Accelerator}: linear scan of {@link Scene#objects} every query.
 * Preserves the pre-refactor behaviour exactly, including the
 * {@code depth == 1 && skipPrimaryRays} filter.
 *
 * <p>This is the only implementation today; a future BVH/KD-tree variant slots in
 * via the same interface without touching the {@link com.raytracer.RayTracer hot
 * path}.
 */
public final class LinearAccelerator implements Accelerator {

    private Scene scene;

    @Override
    public void build(Scene scene) {
        this.scene = scene;
    }

    @Override
    public int nearest(Ray ray, int depth, double[] outIntersect) {
        int objectIndex = -1;
        double nearestT = Double.POSITIVE_INFINITY;
        double[] cand = new double[3];

        for (int i = 0; i < scene.numActive; i++) {
            SceneObject obj = scene.objects[i];
            if (obj.primitive == null) continue;
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
}
