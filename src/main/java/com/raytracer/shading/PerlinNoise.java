package com.raytracer.shading;

import com.raytracer.VecMath;

import java.util.Random;

/**
 * Classic 3-D Perlin noise. Permutation table {@code P} and gradient table {@code G3} are
 * populated once with {@code new Random(12345L)} so output is deterministic across runs.
 * (The original C++ re-initialised these tables on every {@code noise()} call — harmless
 * because the same seed produced the same tables, but wasteful.)
 *
 * <p>Currently has no callers — kept for parity with the original asset library and for
 * future textures.
 */
public final class PerlinNoise {

    private PerlinNoise() {}

    private static final int B  = 0x100;
    private static final int BM = 0xff;
    private static final int NN = 0x1000;

    private static final int[]      P  = new int   [B + B + 2];
    private static final double[][] G3 = new double[B + B + 2][3];

    static {
        Random rng = new Random(12345L);

        for (int i = 0; i < B; i++) {
            P[i] = i;
            for (int j = 0; j < 3; j++)
                G3[i][j] = (double)(rng.nextInt(B + B) - B) / B;
            VecMath.normalize(G3[i]);
        }

        for (int i = B - 1; i >= 1; i--) {
            int j   = rng.nextInt(B);
            int tmp = P[i];
            P[i] = P[j];
            P[j] = tmp;
        }

        for (int i = 0; i < B + 2; i++) {
            P [B + i]    = P[i];
            G3[B + i][0] = G3[i][0];
            G3[B + i][1] = G3[i][1];
            G3[B + i][2] = G3[i][2];
        }
    }

    /** Sample 3-D Perlin noise at world-space point {@code vec}. Result is roughly in {@code [-1, 1]}. */
    public static double noise(double[] vec) {
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
}
