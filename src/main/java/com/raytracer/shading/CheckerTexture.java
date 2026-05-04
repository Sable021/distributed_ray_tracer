package com.raytracer.shading;

import com.raytracer.VecMath;

/**
 * Sin-warped checkerboard used on the floor. Each axis is run through {@code sin()} before
 * the 5× scale + integer-tile quantisation, giving a slightly distorted square grid that
 * mirrors the original C++ {@code mixChecks} routine exactly.
 */
public final class CheckerTexture implements Texture {

    @Override
    public void sample(double[] intersect, double[] colour) {
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
}
