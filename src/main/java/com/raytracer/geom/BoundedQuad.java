package com.raytracer.geom;

import com.raytracer.Ray;
import com.raytracer.VecMath;

/**
 * Plane clipped to a rectangular region defined by two opposite corners. Used to model
 * area lights (the C++ scene's indices 15/16): the underlying plane is infinite, but a
 * hit is rejected if its point on the plane falls outside the corner extents along any
 * axis where the normal vanishes.
 *
 * <p>This type replaces the {@code Scene.isBoundedQuad(idx)} magic-index check that
 * lived in {@code RayTracer.intersectObject}: bounded quads are now their own type,
 * and the clipping logic is encapsulated here.
 */
public record BoundedQuad(double[] normal, double dist, double[] cornerLo, double[] cornerHi)
        implements Primitive {

    public BoundedQuad {
        normal   = normal.clone();
        cornerLo = cornerLo.clone();
        cornerHi = cornerHi.clone();
    }

    @Override
    public double intersect(Ray ray) {
        double denom = VecMath.dot(normal, ray.direct);
        if (denom == 0.0) return -1.0;

        double numer = -(dist + VecMath.dot(normal, ray.point));
        double t     = numer / denom;
        if (t < EPSILON) return -1.0;

        double[] pt = new double[3];
        VecMath.pointOnLine(pt, ray.point, ray.direct, t);

        // Along each axis where the plane extends (normal component is 0), the
        // intersection must lie between cornerLo and cornerHi on that axis.
        for (int k = 0; k < 3; k++) {
            if (normal[k] == 0.0) {
                double lo = Math.min(cornerLo[k], cornerHi[k]);
                double hi = Math.max(cornerLo[k], cornerHi[k]);
                if (pt[k] < lo || pt[k] > hi) return -1.0;
            }
        }
        return t;
    }

    @Override
    public void normalAt(double[] point, double[] outNormal) {
        outNormal[0] = normal[0];
        outNormal[1] = normal[1];
        outNormal[2] = normal[2];
        VecMath.normalize(outNormal);
    }
}
