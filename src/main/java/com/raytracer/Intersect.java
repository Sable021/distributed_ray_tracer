package com.raytracer;

public final class Intersect {

    private Intersect() {}

    private static final double EPSILON = 0.0001;

    // -------------------------------------------------------------------------
    // Intersection tests — return parameter t, or -1.0 if no hit
    // -------------------------------------------------------------------------

    public static double raySphereIntersect(Ray ray, SceneObject sphere) {
        double[] V = new double[3];
        // V = ray.point - sphere.centre
        VecMath.direction(V, sphere.vectors[0], ray.point);

        double b   = 2 * VecMath.dot(ray.direct, V);
        // Preserves C++ formula: c = dot(V,V) - 2*r^2  (not dot(V,V) - r^2)
        double c   = VecMath.dot(V, V) - 2 * sphere.radius * sphere.radius;
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

    /** Möller–Trumbore via Cramer's rule with barycentric test */
    public static double rayTriIntersect(Ray ray, SceneObject tri) {
        double[][] mA     = new double[3][3];
        double[][] mBeta  = new double[3][3];
        double[][] mGamma = new double[3][3];
        double[][] mT     = new double[3][3];

        for (int i = 0; i < 3; i++) mA    [i][0] = tri.vectors[0][i] - tri.vectors[1][i];
        for (int i = 0; i < 3; i++) mA    [i][1] = tri.vectors[0][i] - tri.vectors[2][i];
        for (int i = 0; i < 3; i++) mA    [i][2] = ray.direct[i];

        for (int i = 0; i < 3; i++) mBeta [i][0] = tri.vectors[0][i] - ray.point[i];
        for (int i = 0; i < 3; i++) mBeta [i][1] = tri.vectors[0][i] - tri.vectors[2][i];
        for (int i = 0; i < 3; i++) mBeta [i][2] = ray.direct[i];

        for (int i = 0; i < 3; i++) mGamma[i][0] = tri.vectors[0][i] - tri.vectors[1][i];
        for (int i = 0; i < 3; i++) mGamma[i][1] = tri.vectors[0][i] - ray.point[i];
        for (int i = 0; i < 3; i++) mGamma[i][2] = ray.direct[i];

        for (int i = 0; i < 3; i++) mT    [i][0] = tri.vectors[0][i] - tri.vectors[1][i];
        for (int i = 0; i < 3; i++) mT    [i][1] = tri.vectors[0][i] - tri.vectors[2][i];
        for (int i = 0; i < 3; i++) mT    [i][2] = tri.vectors[0][i] - ray.point[i];

        double detA  = VecMath.det3(mA);
        double beta  = VecMath.det3(mBeta)  / detA;
        double gamma = VecMath.det3(mGamma) / detA;
        double t     = VecMath.det3(mT)     / detA;

        if (beta + gamma < 1.0 && beta > 0.0 && gamma > 0.0 && t > 0.0) {
            return t < EPSILON ? -1.0 : t;
        }
        return -1.0;
    }

    public static double rayPlaneIntersect(Ray ray, SceneObject plane) {
        double denom = VecMath.dot(plane.vectors[0], ray.direct);
        if (denom == 0.0) return -1.0;  // parallel

        double numer = -(plane.dist + VecMath.dot(plane.vectors[0], ray.point));
        double t     = numer / denom;
        return t < EPSILON ? -1.0 : t;
    }

    // -------------------------------------------------------------------------
    // Normal, reflection, refraction
    // -------------------------------------------------------------------------

    public static void getNormal(SceneObject obj, double[] intersect, double[] outNormal) {
        switch (obj.type) {
            case PLANE    -> VecMath.copy(obj.vectors[0], outNormal);
            case TRIANGLE -> VecMath.copy(obj.vectors[3], outNormal);
            case SPHERE   -> {
                outNormal[0] = (intersect[0] - obj.vectors[0][0]) / obj.radius;
                outNormal[1] = (intersect[1] - obj.vectors[0][1]) / obj.radius;
                outNormal[2] = (intersect[2] - obj.vectors[0][2]) / obj.radius;
            }
            default -> System.err.println("getNormal: object is UNASSIGNED");
        }
        VecMath.normalize(outNormal);
    }

    public static void reflection(double[] incident, double[] normal, double[] outRefl) {
        double iDotN = VecMath.dot(incident, normal);
        outRefl[0] = incident[0] - 2 * iDotN * normal[0];
        outRefl[1] = incident[1] - 2 * iDotN * normal[1];
        outRefl[2] = incident[2] - 2 * iDotN * normal[2];
        VecMath.normalize(outRefl);
    }

    /** Returns false for total internal reflection (caller should fall back to reflection) */
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
