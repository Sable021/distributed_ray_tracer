package com.raytracer;

import java.util.Random;

public final class Textures {

    private Textures() {}

    // -------------------------------------------------------------------------
    // Perlin noise tables — initialized once with a fixed seed
    // -------------------------------------------------------------------------

    private static final int B  = 0x100;     // 256
    private static final int BM = 0xff;      // 255
    private static final int NN = 0x1000;    // 4096  (called N in C++; renamed to avoid clash)

    private static final int[]      P  = new int   [B + B + 2];
    private static final double[][] G3 = new double[B + B + 2][3];

    static {
        Random rng = new Random(12345L);

        // Fill P[0..B-1] = identity, G3[0..B-1] = random unit vectors
        for (int i = 0; i < B; i++) {
            P[i] = i;
            for (int j = 0; j < 3; j++)
                G3[i][j] = (double)(rng.nextInt(B + B) - B) / B;
            VecMath.normalize(G3[i]);
        }

        // Fisher-Yates shuffle of P[1..B-1] (C++ shuffles i1=B-1 down to 1)
        for (int i = B - 1; i >= 1; i--) {
            int j   = rng.nextInt(B);
            int tmp = P[i];
            P[i] = P[j];
            P[j] = tmp;
        }

        // Mirror into the second half to avoid modular indexing during lookup
        for (int i = 0; i < B + 2; i++) {
            P [B + i]    = P[i];
            G3[B + i][0] = G3[i][0];
            G3[B + i][1] = G3[i][1];
            G3[B + i][2] = G3[i][2];
        }
    }

    // -------------------------------------------------------------------------
    // Noise function (3-D Perlin noise)
    // -------------------------------------------------------------------------

    public static double noise(double[] vec) {
        // --- setup: fractional decomposition for each axis ---
        double tx = vec[0] + NN;
        int bx0 = ((int) tx) & BM, bx1 = (bx0 + 1) & BM;
        double rx0 = tx - (int) tx,  rx1 = rx0 - 1.0;

        double ty = vec[1] + NN;
        int by0 = ((int) ty) & BM, by1 = (by0 + 1) & BM;
        double ry0 = ty - (int) ty,  ry1 = ry0 - 1.0;

        double tz = vec[2] + NN;
        int bz0 = ((int) tz) & BM, bz1 = (bz0 + 1) & BM;
        double rz0 = tz - (int) tz,  rz1 = rz0 - 1.0;

        int ii = P[bx0], jj = P[bx1];
        int b00 = P[ii + by0], b10 = P[jj + by0],
            b01 = P[ii + by1], b11 = P[jj + by1];

        double sx = sCurve(rx0), sy = sCurve(ry0), sz = sCurve(rz0);

        // --- trilinear interpolation using gradient dot products ---
        double u, v, a, b, c, d;

        u = at3(G3[b00 + bz0], rx0, ry0, rz0);
        v = at3(G3[b10 + bz0], rx1, ry0, rz0);
        a = lerp(sx, u, v);

        u = at3(G3[b01 + bz0], rx0, ry1, rz0);
        v = at3(G3[b11 + bz0], rx1, ry1, rz0);
        b = lerp(sx, u, v);

        c = lerp(sy, a, b);

        u = at3(G3[b00 + bz1], rx0, ry0, rz1);
        v = at3(G3[b10 + bz1], rx1, ry0, rz1);
        a = lerp(sx, u, v);

        u = at3(G3[b01 + bz1], rx0, ry1, rz1);
        v = at3(G3[b11 + bz1], rx1, ry1, rz1);
        b = lerp(sx, u, v);

        d = lerp(sy, a, b);

        return lerp(sz, c, d);
    }

    private static double sCurve(double t) { return t * t * (3.0 - 2.0 * t); }
    private static double lerp(double t, double a, double b) { return a + t * (b - a); }
    private static double at3(double[] q, double rx, double ry, double rz) {
        return rx * q[0] + ry * q[1] + rz * q[2];
    }

    // -------------------------------------------------------------------------
    // Procedural texture functions (out-param colour[3])
    // -------------------------------------------------------------------------

    public static void checkerboard(double[] intersect, double[] colour) {
        int cx = (int)(5 * intersect[0]);
        int cy = (int)(5 * intersect[1]);
        int cz = (int)(5 * intersect[2]);

        boolean xEven = cx % 2 == 0, yEven = cy % 2 == 0, zEven = cz % 2 == 0;
        boolean xyEven = xEven && yEven;

        if (intersect[0] >= 0) {
            if (zEven) {
                VecMath.set(colour, xyEven ? 0.5 : 0.0, xyEven ? 0.5 : 0.25, xyEven ? 0.5 : 0.4);
            } else {
                VecMath.set(colour, xyEven ? 0.0 : 0.5, xyEven ? 0.25 : 0.5, xyEven ? 0.4 : 0.5);
            }
        } else {
            if (zEven) {
                VecMath.set(colour, xyEven ? 0.0 : 0.5, xyEven ? 0.25 : 0.5, xyEven ? 0.4 : 0.5);
            } else {
                VecMath.set(colour, xyEven ? 0.5 : 0.0, xyEven ? 0.5 : 0.25, xyEven ? 0.5 : 0.4);
            }
        }
    }

    /** Checkerboard with sin-perturbation of the intersection point */
    public static void mixChecks(double[] intersect, double[] colour) {
        double[] tp = new double[]{
            Math.sin(intersect[0]),
            Math.sin(intersect[1]),
            Math.sin(intersect[2])
        };

        int cx = (int)(5 * tp[0]);
        int cy = (int)(5 * tp[1]);
        int cz = (int)(5 * tp[2]);

        boolean xEven = cx % 2 == 0, yEven = cy % 2 == 0, zEven = cz % 2 == 0;
        boolean xyEven = xEven && yEven;

        if (tp[0] >= 0) {
            if (zEven) {
                VecMath.set(colour, xyEven ? 0.4 : 0.0, xyEven ? 0.4 : 0.15, xyEven ? 0.4 : 0.3);
            } else {
                VecMath.set(colour, xyEven ? 0.0 : 0.4, xyEven ? 0.15 : 0.4, xyEven ? 0.3 : 0.4);
            }
        } else {
            if (zEven) {
                VecMath.set(colour, xyEven ? 0.0 : 0.4, xyEven ? 0.15 : 0.4, xyEven ? 0.3 : 0.4);
            } else {
                VecMath.set(colour, xyEven ? 0.4 : 0.0, xyEven ? 0.4 : 0.15, xyEven ? 0.4 : 0.3);
            }
        }
    }

    /**
     * Map normalized intersect direction to |sin(component)| per channel.
     * NOTE: mutates intersect in place (normalizes it) — matches C++ behaviour.
     */
    public static void colourful(double[] intersect, double[] colour) {
        VecMath.normalize(intersect);
        for (int i = 0; i < 3; i++)
            colour[i] = Math.abs(Math.sin(intersect[i]));
    }

    /** Wood-grain-style stripe texture used on the tetrahedron */
    public static void strips(double[] intersect, double[] colour) {
        final double R_LIGHT = 0.82, G_LIGHT = 0.67, B_LIGHT = 0.56;
        final double R_DARK  = 0.52, G_DARK  = 0.36, B_DARK  = 0.25;

        double[] tp = new double[3];
        for (int i = 0; i < 3; i++) tp[i] = Math.abs(Math.sin(intersect[i]));
        for (int i = 0; i < 3; i++) tp[i] = Math.abs(Math.sin(tp[i]));

        double radius = Math.sqrt(tp[0]*tp[0] + tp[1]*tp[1]);
        double angle  = (tp[2] == 0) ? Math.PI / 2 : Math.atan2(tp[0], tp[2]);
        if (angle < 0) angle += 2 * Math.PI;

        radius = radius + 2 * Math.sin(20 * angle + tp[1] / 150.0);
        int grain = (int)(radius + 0.5) % 5;

        if (grain < 1) {
            VecMath.set(colour, R_LIGHT, G_LIGHT, B_LIGHT);
        } else {
            VecMath.set(colour, R_DARK, G_DARK, B_DARK);
        }
    }
}
