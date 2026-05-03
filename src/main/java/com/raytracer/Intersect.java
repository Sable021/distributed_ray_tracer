package com.raytracer;

/**
 * Reflection and refraction helpers. Per-primitive intersection and surface-normal
 * computation now live on each {@link com.raytracer.geom.Primitive Primitive} impl;
 * this class is a thin holder for the algorithm-side ray operations that don't depend
 * on geometry type.
 */
public final class Intersect {

    private Intersect() {}

    /**
     * Mirror-reflection direction for an incident ray hitting a surface with the given normal.
     * Computed as {@code incident - 2 * (incident · normal) * normal} and normalized.
     */
    public static void reflection(double[] incident, double[] normal, double[] outRefl) {
        double iDotN = VecMath.dot(incident, normal);
        outRefl[0] = incident[0] - 2 * iDotN * normal[0];
        outRefl[1] = incident[1] - 2 * iDotN * normal[1];
        outRefl[2] = incident[2] - 2 * iDotN * normal[2];
        VecMath.normalize(outRefl);
    }

    /**
     * Snell's-law refracted direction for a ray crossing from a medium with refractive index
     * {@code indexI} into one with index {@code indexR}.
     *
     * <p>Returns {@code false} if the angle exceeds the critical angle for the index ratio,
     * indicating total internal reflection. In that case the caller should fall back to
     * mirror reflection ({@link #reflection}).
     */
    public static boolean refraction(double[] incident, double[] normal,
                                     double indexI, double indexR, double[] outRefr) {
        double iDotN  = VecMath.dot(incident, normal);
        double u      = indexI / indexR;
        double sqCoef = 1.0 - u * u * (1.0 - iDotN * iDotN);

        if (sqCoef < 0.0) return false;

        double cosTheta = Math.sqrt(sqCoef);
        double cosPhi   = -iDotN;
        double nCoeff   = cosTheta + u * cosPhi;

        outRefr[0] = u * incident[0] - nCoeff * normal[0];
        outRefr[1] = u * incident[1] - nCoeff * normal[1];
        outRefr[2] = u * incident[2] - nCoeff * normal[2];
        VecMath.normalize(outRefr);
        return true;
    }
}
