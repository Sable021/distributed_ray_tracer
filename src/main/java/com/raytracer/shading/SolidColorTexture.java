package com.raytracer.shading;

import com.raytracer.VecMath;

/** Constant-colour texture: every sample returns the same RGB triple. */
public final class SolidColorTexture implements Texture {

    private final double[] rgb;

    public SolidColorTexture(double r, double g, double b) {
        this.rgb = new double[]{ r, g, b };
    }

    public SolidColorTexture(double[] rgb) {
        this.rgb = rgb.clone();
    }

    @Override
    public void sample(double[] point, double[] outRgb) {
        VecMath.copy(rgb, outRgb);
    }
}
