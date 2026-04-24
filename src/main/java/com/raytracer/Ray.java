package com.raytracer;

public class Ray {
    public final double[] point  = new double[3];
    public final double[] direct = new double[3];

    public Ray() {}

    public static Ray make(double[] point, double[] direct) {
        Ray r = new Ray();
        VecMath.copy(point,  r.point);
        VecMath.copy(direct, r.direct);
        return r;
    }
}
