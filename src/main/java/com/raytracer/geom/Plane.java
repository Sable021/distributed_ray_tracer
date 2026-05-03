package com.raytracer.geom;

import com.raytracer.Ray;
import com.raytracer.VecMath;

/**
 * Infinite plane defined by a unit normal and signed distance from the origin along it.
 * Hit when {@code dot(normal, ray.direct) != 0} and the resulting {@code t > EPSILON}.
 */
public record Plane(double[] normal, double dist) implements Primitive {

    public Plane {
        normal = normal.clone();
    }

    @Override
    public double intersect(Ray ray) {
        double denom = VecMath.dot(normal, ray.direct);
        if (denom == 0.0) return -1.0;

        double numer = -(dist + VecMath.dot(normal, ray.point));
        double t     = numer / denom;
        return t < EPSILON ? -1.0 : t;
    }

    @Override
    public void normalAt(double[] point, double[] outNormal) {
        outNormal[0] = normal[0];
        outNormal[1] = normal[1];
        outNormal[2] = normal[2];
        VecMath.normalize(outNormal);
    }
}
