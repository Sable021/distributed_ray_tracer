package com.raytracer;

/**
 * A 3-D ray defined by an origin {@link #point} and a (typically normalized) direction
 * {@link #direct}.
 *
 * <p>Mutable by design: callers reuse Ray instances or construct them via the
 * {@link #make(double[], double[])} factory to copy in caller-owned buffers without
 * aliasing.
 */
public class Ray {
    /** Ray origin in world space. */
    public final double[] point  = new double[3];
    /** Ray direction in world space. Expected to be unit length for shading correctness. */
    public final double[] direct = new double[3];

    public Ray() {}

    /**
     * Build a Ray by deep-copying the supplied origin and direction. Use this when the
     * caller's buffers may be mutated after construction (e.g. in the inner pixel loop).
     */
    public static Ray make(double[] point, double[] direct) {
        Ray r = new Ray();
        VecMath.copy(point,  r.point);
        VecMath.copy(direct, r.direct);
        return r;
    }
}
