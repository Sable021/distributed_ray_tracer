package com.raytracer.render;

import com.raytracer.Ray;
import com.raytracer.VecMath;

/**
 * Standard supersampled pinhole-camera strategy: jittered NxN rays per pixel, all
 * originating at the eye and aimed through points stratified across the screen-plane cell.
 *
 * <p>This is the body that lived in {@code Renderer.renderSupersampled} pre-Phase-E, lifted
 * out unchanged to satisfy {@link RenderStrategy}. The accumulator divides by
 * {@code gridX * gridY} so the caller can pack the result directly without a second
 * normalisation step.
 */
public final class PinholeStrategy implements RenderStrategy {

    private final double[] eye;
    private final double scrZ;
    private final PathIntegrator integrator;
    private final RandomSource rng;

    /**
     * @param eye        camera origin (cloned)
     * @param scrZ       z-coordinate of the screen plane
     * @param integrator integrator used to shade each sample ray
     * @param rng        random source the {@link com.raytracer.Renderer} reseeds per row
     */
    public PinholeStrategy(double[] eye, double scrZ, PathIntegrator integrator, RandomSource rng) {
        this.eye        = eye.clone();
        this.scrZ       = scrZ;
        this.integrator = integrator;
        this.rng        = rng;
    }

    @Override
    public void shadePixel(double scrX, double scrY, double scrDX, double scrDY,
                           int gridX, int gridY, double[] outRgb) {
        int gridSize = gridX * gridY;
        double cellDX = scrDX / gridX;
        double cellDY = scrDY / gridY;

        double accR = 0.0, accG = 0.0, accB = 0.0;

        for (int k = 0; k < gridY; k++) {
            for (int l = 0; l < gridX; l++) {
                double[] sub = {
                    scrX + cellDX * l + rng.uniform(0.0, cellDX),
                    scrY + cellDY * k + rng.uniform(0.0, cellDY),
                    scrZ
                };
                double[] dir = VecMath.direction(eye, sub);
                VecMath.normalize(dir);
                Ray ray = Ray.make(eye, dir);

                double[] c = new double[3];
                integrator.trace(ray, 1, 1.0, c, false, k * gridX + l);
                accR += c[0]; accG += c[1]; accB += c[2];
            }
        }

        outRgb[0] = accR / gridSize;
        outRgb[1] = accG / gridSize;
        outRgb[2] = accB / gridSize;
    }
}
