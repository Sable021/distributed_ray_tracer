package com.raytracer;

public final class VecMath {

    private VecMath() {}

    /** out = v2 - v1 (not normalized) */
    public static void direction(double[] out, double[] v1, double[] v2) {
        out[0] = v2[0] - v1[0];
        out[1] = v2[1] - v1[1];
        out[2] = v2[2] - v1[2];
    }

    /** Allocating variant: returns v2 - v1 */
    public static double[] direction(double[] v1, double[] v2) {
        return new double[]{v2[0]-v1[0], v2[1]-v1[1], v2[2]-v1[2]};
    }

    /** out = a × b; result is normalized (matches C++ cal_cross_product behavior) */
    public static void cross(double[] out, double[] a, double[] b) {
        out[0] = a[1]*b[2] - a[2]*b[1];
        out[1] = a[2]*b[0] - a[0]*b[2];
        out[2] = a[0]*b[1] - a[1]*b[0];
        normalize(out);
    }

    public static double dot(double[] a, double[] b) {
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
    }

    /** 3×3 determinant; m is indexed [row][col] */
    public static double det3(double[][] m) {
        return m[0][0]*m[1][1]*m[2][2] + m[0][1]*m[1][2]*m[2][0]
             + m[0][2]*m[1][0]*m[2][1] - m[0][2]*m[1][1]*m[2][0]
             - m[0][0]*m[1][2]*m[2][1] - m[0][1]*m[1][0]*m[2][2];
    }

    public static double magnitude(double[] v) {
        return Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
    }

    /** out = origin + t*dir */
    public static void pointOnLine(double[] out, double[] origin, double[] dir, double t) {
        out[0] = origin[0] + t*dir[0];
        out[1] = origin[1] + t*dir[1];
        out[2] = origin[2] + t*dir[2];
    }

    /** Normalize v in place; skips if already ~unit (preserves C++ epsilon tolerance) */
    public static void normalize(double[] v) {
        double mag = magnitude(v);
        if (mag < 0.999999999 || mag > 1.000000001) {
            v[0] /= mag;
            v[1] /= mag;
            v[2] /= mag;
        }
    }

    /** Copy src into dst */
    public static void copy(double[] src, double[] dst) {
        dst[0] = src[0];
        dst[1] = src[1];
        dst[2] = src[2];
    }

    public static void set(double[] v, double a, double b, double c) {
        v[0] = a;
        v[1] = b;
        v[2] = c;
    }
}
