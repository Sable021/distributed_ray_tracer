package com.raytracer.render;

import com.raytracer.Ray;
import com.raytracer.VecMath;
import com.raytracer.geom.Plane;

/**
 * Depth-of-field strategy: rays originate from jittered points on a square lens aperture
 * centred on the pixel and aim at the focus point where the eye-through-pixel ray hits a
 * synthetic focal {@link Plane} at {@code z = dofFocalDist} (built with normal
 * {@code (0,0,-1)}). Objects on the focal plane stay sharp; the rest defocuses into bokeh.
 *
 * <p>This is the body that lived in {@code Renderer.renderDepthOfField} pre-Phase-E. The
 * focal {@code Plane} is built once per strategy instance (it depends only on
 * {@code dofFocalDist}, which is fixed per render). When the eye-through-pixel ray fails
 * to hit the plane, {@code outRgb} is set to {@code (0,0,0)} — bit-identical to the legacy
 * code's direct {@code 0xFF000000} write because {@code packArgb} maps black to that ARGB.
 */
public final class DepthOfFieldStrategy implements RenderStrategy {

    private final double[] eye;
    private final double scrZ;
    private final double dofLensWidth, dofLensHeight;
    private final Plane focalPlane;
    private final PathIntegrator integrator;
    private final RandomSource rng;

    /**
     * @param eye           camera origin (cloned)
     * @param scrZ          z-coordinate of the screen plane (also the lens plane)
     * @param dofLensWidth  aperture width
     * @param dofLensHeight aperture height
     * @param dofFocalDist  z-coordinate of the focal plane (built with normal (0,0,-1));
     *                      the eye-through-pixel ray hits it iff {@code dofFocalDist < scrZ}
     * @param integrator    integrator used to shade each sample ray
     * @param rng           random source the {@link com.raytracer.Renderer} reseeds per row
     */
    public DepthOfFieldStrategy(double[] eye, double scrZ,
                                double dofLensWidth, double dofLensHeight, double dofFocalDist,
                                PathIntegrator integrator, RandomSource rng) {
        this.eye           = eye.clone();
        this.scrZ          = scrZ;
        this.dofLensWidth  = dofLensWidth;
        this.dofLensHeight = dofLensHeight;
        this.focalPlane    = new Plane(new double[]{0.0, 0.0, -1.0}, dofFocalDist);
        this.integrator    = integrator;
        this.rng           = rng;
    }

    @Override
    public void shadePixel(double scrX, double scrY, double scrDX, double scrDY,
                           int gridX, int gridY, double[] outRgb) {
        double dofDX = dofLensWidth  / gridX;
        double dofDY = dofLensHeight / gridY;
        int gridSize = gridX * gridY;

        double[] pixelPt = { scrX, scrY, scrZ };

        // Eye-through-pixel ray finds the focus point on the focal plane.
        double[] dofDir = VecMath.direction(eye, pixelPt);
        VecMath.normalize(dofDir);
        Ray dofRay = Ray.make(pixelPt, dofDir);

        double tFocus = focalPlane.intersect(dofRay);
        if (tFocus <= 0.0) {
            outRgb[0] = 0.0; outRgb[1] = 0.0; outRgb[2] = 0.0;
            return;
        }

        double[] focusPt = new double[3];
        VecMath.pointOnLine(focusPt, dofRay.point, dofRay.direct, tFocus);

        double lensOriginX = scrX - dofLensWidth  / 2.0;
        double lensOriginY = scrY - dofLensHeight / 2.0;

        double accR = 0.0, accG = 0.0, accB = 0.0;

        for (int k = 0; k < gridY; k++) {
            for (int l = 0; l < gridX; l++) {
                double[] lensPt = {
                    lensOriginX + dofDX * l + rng.uniform(0.0, dofDX),
                    lensOriginY + dofDY * k + rng.uniform(0.0, dofDY),
                    scrZ
                };
                double[] dir = VecMath.direction(lensPt, focusPt);
                VecMath.normalize(dir);
                Ray ray = Ray.make(lensPt, dir);

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
