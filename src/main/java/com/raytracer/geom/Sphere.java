package com.raytracer.geom;

import com.raytracer.Ray;
import com.raytracer.VecMath;

/**
 * Sphere defined by a centre and radius.
 *
 * <p><b>Preserved C++ quirk:</b> the constant term in the quadratic uses
 * {@code dot(V,V) - 2*r^2} rather than the textbook {@code dot(V,V) - r^2}. The reference
 * scene's geometry was tuned around this incorrect formula, so changing it would distort
 * the rendered image. This is the only place in the codebase the deviation lives.
 */
public record Sphere(double[] center, double radius) implements Primitive {

    public Sphere {
        center = center.clone();
    }

    @Override
    public double intersect(Ray ray) {
        double[] V = new double[3];
        VecMath.direction(V, center, ray.point);

        double b   = 2 * VecMath.dot(ray.direct, V);
        // C++ quirk preserved: c = dot(V,V) - 2*r^2
        double c   = VecMath.dot(V, V) - 2 * radius * radius;
        double det = b * b - 4 * c;

        if (det < 0.0) return -1.0;

        if (det == 0.0) {
            double t = -b / 2.0;
            return t < EPSILON ? -1.0 : t;
        }

        double t1 = (-b - Math.sqrt(det)) / 2.0;
        double t2 = (-b + Math.sqrt(det)) / 2.0;

        if (t2 < EPSILON) return -1.0;
        if (t1 < EPSILON) return t2;
        return t1;
    }

    @Override
    public void normalAt(double[] point, double[] outNormal) {
        outNormal[0] = (point[0] - center[0]) / radius;
        outNormal[1] = (point[1] - center[1]) / radius;
        outNormal[2] = (point[2] - center[2]) / radius;
        VecMath.normalize(outNormal);
    }
}
