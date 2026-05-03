package com.raytracer.geom;

import com.raytracer.Ray;
import com.raytracer.VecMath;

/**
 * Triangle defined by three vertices (anticlockwise winding) and a pre-computed unit
 * surface normal. Intersection by Cramer's rule on the 3-unknown barycentric system
 * {@code v0 + beta*(v1-v0) + gamma*(v2-v0) = origin + t*direction}.
 */
public record Triangle(double[] v0, double[] v1, double[] v2, double[] normal) implements Primitive {

    public Triangle {
        v0     = v0.clone();
        v1     = v1.clone();
        v2     = v2.clone();
        normal = normal.clone();
    }

    @Override
    public double intersect(Ray ray) {
        double[][] mA     = new double[3][3];
        double[][] mBeta  = new double[3][3];
        double[][] mGamma = new double[3][3];
        double[][] mT     = new double[3][3];

        for (int i = 0; i < 3; i++) mA    [i][0] = v0[i] - v1[i];
        for (int i = 0; i < 3; i++) mA    [i][1] = v0[i] - v2[i];
        for (int i = 0; i < 3; i++) mA    [i][2] = ray.direct[i];

        for (int i = 0; i < 3; i++) mBeta [i][0] = v0[i] - ray.point[i];
        for (int i = 0; i < 3; i++) mBeta [i][1] = v0[i] - v2[i];
        for (int i = 0; i < 3; i++) mBeta [i][2] = ray.direct[i];

        for (int i = 0; i < 3; i++) mGamma[i][0] = v0[i] - v1[i];
        for (int i = 0; i < 3; i++) mGamma[i][1] = v0[i] - ray.point[i];
        for (int i = 0; i < 3; i++) mGamma[i][2] = ray.direct[i];

        for (int i = 0; i < 3; i++) mT    [i][0] = v0[i] - v1[i];
        for (int i = 0; i < 3; i++) mT    [i][1] = v0[i] - v2[i];
        for (int i = 0; i < 3; i++) mT    [i][2] = v0[i] - ray.point[i];

        double detA  = VecMath.det3(mA);
        double beta  = VecMath.det3(mBeta)  / detA;
        double gamma = VecMath.det3(mGamma) / detA;
        double t     = VecMath.det3(mT)     / detA;

        if (beta + gamma < 1.0 && beta > 0.0 && gamma > 0.0 && t > 0.0) {
            return t < EPSILON ? -1.0 : t;
        }
        return -1.0;
    }

    @Override
    public void normalAt(double[] point, double[] outNormal) {
        outNormal[0] = normal[0];
        outNormal[1] = normal[1];
        outNormal[2] = normal[2];
        VecMath.normalize(outNormal);
    }
}
