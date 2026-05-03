package com.raytracer.geom;

import com.raytracer.Ray;
import com.raytracer.VecMath;

/**
 * Finite cylinder defined by a centre (midpoint of the axis), a unit axis vector, a
 * radius, and a half-height along the axis. Tests the infinite-cylinder quadratic for
 * side hits, clipped to {@code ±halfHeight}, then both end-cap discs. Returns the
 * smallest positive {@code t}.
 */
public record Cylinder(double[] center, double[] axis, double radius, double halfHeight)
        implements Primitive {

    public Cylinder {
        center = center.clone();
        axis   = axis.clone();
    }

    @Override
    public double intersect(Ray ray) {
        double[] O = ray.point, D = ray.direct;
        double[] C = center,    A = axis;
        double r = radius, halfH = halfHeight;

        double dx = O[0]-C[0], dy = O[1]-C[1], dz = O[2]-C[2];
        double DdotA   = D[0]*A[0] + D[1]*A[1] + D[2]*A[2];
        double deldotA = dx*A[0]   + dy*A[1]   + dz*A[2];

        double dpx = D[0]-DdotA*A[0], dpy = D[1]-DdotA*A[1], dpz = D[2]-DdotA*A[2];
        double epx = dx-deldotA*A[0], epy = dy-deldotA*A[1], epz = dz-deldotA*A[2];

        double qa = dpx*dpx + dpy*dpy + dpz*dpz;
        double qb = dpx*epx + dpy*epy + dpz*epz;
        double qc = epx*epx + epy*epy + epz*epz - r*r;

        double best = Double.POSITIVE_INFINITY;

        if (qa > 1e-12) {
            double disc = qb*qb - qa*qc;
            if (disc >= 0) {
                double sq = Math.sqrt(disc);
                for (double t : new double[]{ (-qb-sq)/qa, (-qb+sq)/qa }) {
                    if (t > EPSILON && Math.abs(deldotA + t*DdotA) <= halfH && t < best)
                        best = t;
                }
            }
        }

        if (Math.abs(DdotA) > 1e-12) {
            for (int sign : new int[]{-1, 1}) {
                double t = (sign * halfH - deldotA) / DdotA;
                if (t > EPSILON && t < best) {
                    double px = dx+t*D[0]-sign*halfH*A[0];
                    double py = dy+t*D[1]-sign*halfH*A[1];
                    double pz = dz+t*D[2]-sign*halfH*A[2];
                    if (px*px + py*py + pz*pz <= r*r) best = t;
                }
            }
        }

        return best == Double.POSITIVE_INFINITY ? -1.0 : best;
    }

    @Override
    public void normalAt(double[] point, double[] outNormal) {
        double[] C = center, A = axis;
        double dx = point[0]-C[0], dy = point[1]-C[1], dz = point[2]-C[2];
        double proj = dx*A[0] + dy*A[1] + dz*A[2];
        if (Math.abs(Math.abs(proj) - halfHeight) < 1e-3) {
            double sign = proj > 0 ? 1.0 : -1.0;
            outNormal[0] = sign*A[0]; outNormal[1] = sign*A[1]; outNormal[2] = sign*A[2];
        } else {
            outNormal[0] = dx - proj*A[0];
            outNormal[1] = dy - proj*A[1];
            outNormal[2] = dz - proj*A[2];
        }
        VecMath.normalize(outNormal);
    }
}
