package com.raytracer;

/**
 * Ray–primitive intersection tests, plus surface-normal computation, mirror reflection,
 * and Snell's-law refraction.
 *
 * <p>All intersection methods return the parametric distance {@code t} along the ray
 * ({@code point + t * direct} is the hit), or {@code -1.0} for a miss. The
 * {@link #EPSILON} threshold filters out self-intersections (a reflected/refracted ray
 * leaving an object should not immediately hit the same surface).
 */
public final class Intersect {

    private Intersect() {}

    /** Minimum t-value treated as a real hit; smaller values are discarded as self-intersections. */
    private static final double EPSILON = 0.0001;

    // -------------------------------------------------------------------------
    // Intersection tests — return parameter t, or -1.0 if no hit
    // -------------------------------------------------------------------------

    /**
     * Ray–sphere intersection via the quadratic formula.
     *
     * <p>Preserves a quirk from the original C++ source: the constant term is
     * {@code dot(V,V) - 2*r^2} rather than the textbook {@code dot(V,V) - r^2}. The scene
     * geometry was tuned around this incorrect formula, so changing it would distort the
     * reference image.
     */
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

    /**
     * Ray–triangle intersection by solving the 3-unknown system
     * {@code v0 + beta*(v1-v0) + gamma*(v2-v0) = origin + t*direction} via Cramer's rule.
     * The hit is accepted only when the barycentric coordinates lie inside the triangle
     * ({@code beta > 0}, {@code gamma > 0}, {@code beta + gamma < 1}).
     */
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

    /**
     * Ray–plane intersection. Returns the parametric distance along the ray to the plane
     * defined by {@code plane.vectors[0]} (normal) and {@code plane.dist} (signed distance
     * from origin), or {@code -1.0} if the ray is parallel to the plane or hits behind the
     * origin.
     */
    public static double rayPlaneIntersect(Ray ray, SceneObject plane) {
        double denom = VecMath.dot(plane.vectors[0], ray.direct);
        if (denom == 0.0) return -1.0;  // parallel

        double numer = -(plane.dist + VecMath.dot(plane.vectors[0], ray.point));
        double t     = numer / denom;
        return t < EPSILON ? -1.0 : t;
    }

    /**
     * Ray–cylinder intersection. The cylinder is finite, defined by a centre (midpoint of the
     * axis), a unit axis vector, a radius, and a half-height stored in {@code cyl.dist}.
     *
     * <p>Tests the infinite-cylinder quadratic for side hits, clipped to {@code ±halfH} along
     * the axis, then tests both disc end-caps. Returns the smallest positive {@code t}, or
     * {@code -1.0} on miss.
     */
    public static double rayCylinderIntersect(Ray ray, SceneObject cyl) {
        double[] O = ray.point, D = ray.direct;
        double[] C = cyl.vectors[0], A = cyl.vectors[1];
        double r = cyl.radius, halfH = cyl.dist;

        double dx = O[0]-C[0], dy = O[1]-C[1], dz = O[2]-C[2];
        double DdotA   = D[0]*A[0] + D[1]*A[1] + D[2]*A[2];
        double deldotA = dx*A[0]   + dy*A[1]   + dz*A[2];

        // Components of D and delta perpendicular to the axis
        double dpx = D[0]-DdotA*A[0], dpy = D[1]-DdotA*A[1], dpz = D[2]-DdotA*A[2];
        double epx = dx-deldotA*A[0], epy = dy-deldotA*A[1], epz = dz-deldotA*A[2];

        double qa = dpx*dpx + dpy*dpy + dpz*dpz;
        double qb = dpx*epx + dpy*epy + dpz*epz;
        double qc = epx*epx + epy*epy + epz*epz - r*r;

        double best = Double.POSITIVE_INFINITY;

        // Side surface: solve quadratic, accept if within height bounds
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

        // End caps: intersect ray with each cap plane, accept if within disc radius
        if (Math.abs(DdotA) > 1e-12) {
            for (int sign : new int[]{-1, 1}) {
                double t = (sign * halfH - deldotA) / DdotA;
                if (t > EPSILON && t < best) {
                    // At t_cap the axial component of (P-CC) is exactly zero,
                    // so the full vector magnitude equals the radial distance.
                    double px = dx+t*D[0]-sign*halfH*A[0];
                    double py = dy+t*D[1]-sign*halfH*A[1];
                    double pz = dz+t*D[2]-sign*halfH*A[2];
                    if (px*px + py*py + pz*pz <= r*r) best = t;
                }
            }
        }

        return best == Double.POSITIVE_INFINITY ? -1.0 : best;
    }

    // -------------------------------------------------------------------------
    // Normal, reflection, refraction
    // -------------------------------------------------------------------------

    /**
     * Compute the unit surface normal at {@code intersect} for the given object.
     * Plane and triangle normals are stored on the object directly; sphere normals are
     * derived from {@code (intersect - centre) / radius}.
     */
    public static void getNormal(SceneObject obj, double[] intersect, double[] outNormal) {
        switch (obj.type) {
            case PLANE    -> VecMath.copy(obj.vectors[0], outNormal);
            case TRIANGLE -> VecMath.copy(obj.vectors[3], outNormal);
            case SPHERE   -> {
                outNormal[0] = (intersect[0] - obj.vectors[0][0]) / obj.radius;
                outNormal[1] = (intersect[1] - obj.vectors[0][1]) / obj.radius;
                outNormal[2] = (intersect[2] - obj.vectors[0][2]) / obj.radius;
            }
            case CYLINDER -> {
                double[] C = obj.vectors[0], A = obj.vectors[1];
                double dx = intersect[0]-C[0], dy = intersect[1]-C[1], dz = intersect[2]-C[2];
                double proj = dx*A[0] + dy*A[1] + dz*A[2];
                if (Math.abs(Math.abs(proj) - obj.dist) < 1e-3) {
                    // Cap hit: normal points along ±axis
                    double sign = proj > 0 ? 1.0 : -1.0;
                    outNormal[0] = sign*A[0]; outNormal[1] = sign*A[1]; outNormal[2] = sign*A[2];
                } else {
                    // Side hit: radial normal (perpendicular to axis)
                    outNormal[0] = dx - proj*A[0];
                    outNormal[1] = dy - proj*A[1];
                    outNormal[2] = dz - proj*A[2];
                }
            }
            default -> System.err.println("getNormal: object is UNASSIGNED");
        }
        VecMath.normalize(outNormal);
    }

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
