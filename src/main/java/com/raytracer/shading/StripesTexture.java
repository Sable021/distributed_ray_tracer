package com.raytracer.shading;

import com.raytracer.VecMath;

/**
 * Wood-grain stripe texture used on the four tetrahedron faces. Builds a banded pattern by
 * computing a polar radius from the sin-warped intersect, modulating it with angle, and
 * quantising to one of two preset palette colours.
 */
public final class StripesTexture implements Texture {

    private static final double R_LIGHT = 0.82, G_LIGHT = 0.67, B_LIGHT = 0.56;
    private static final double R_DARK  = 0.52, G_DARK  = 0.36, B_DARK  = 0.25;

    @Override
    public void sample(double[] intersect, double[] colour) {
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
