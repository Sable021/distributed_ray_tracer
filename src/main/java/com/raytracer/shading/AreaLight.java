package com.raytracer.shading;

import com.raytracer.Rng;
import com.raytracer.Sampling;
import com.raytracer.VecMath;

/**
 * Quad-shaped emitter sampled by a stratified grid of jittered shadow rays for soft
 * shadows. Owns its own grid (replacing the pre-Phase-B {@code Sampling.createLightGrid}
 * side-effect on {@code SceneObject}).
 *
 * <p>Construct with the four corners; call {@link #buildGrid(int, int)} once, before any
 * shading, to size and populate the sample grid against the renderer's configured grid
 * dimensions.
 */
public final class AreaLight implements Light {

    private final double[]   diffuseEmission;
    private final double[]   specularEmission;
    private final double[][] corners;          // [4][3]: 0,1 along local X; 0,3 along local Y

    private double[][][] gridSample;           // [y][x][3], allocated by buildGrid
    private final double[] gridDX = new double[3];
    private final double[] gridDY = new double[3];
    private int gridX, gridY;

    public AreaLight(double[] diffuse, double[] specular, double[][] corners) {
        this.diffuseEmission  = diffuse.clone();
        this.specularEmission = specular.clone();
        this.corners = new double[4][3];
        for (int i = 0; i < 4; i++) VecMath.copy(corners[i], this.corners[i]);
    }

    /**
     * Pre-compute the {@code gridY × gridX} grid of sample points across the light's
     * surface. Idempotent for a given {@code (gridX, gridY)} pair; call once per render.
     */
    public void buildGrid(int gridX, int gridY) {
        this.gridX = gridX;
        this.gridY = gridY;
        this.gridSample = new double[gridY][gridX][3];

        double[] directX = new double[3];
        double[] directY = new double[3];
        VecMath.direction(directX, corners[0], corners[1]);
        VecMath.direction(directY, corners[0], corners[3]);

        double lenX = VecMath.magnitude(directX);
        double lenY = VecMath.magnitude(directY);

        VecMath.normalize(directX);
        VecMath.normalize(directY);

        gridDX[0] = (lenX / gridX) * directX[0];
        gridDX[1] = (lenX / gridX) * directX[1];
        gridDX[2] = (lenX / gridX) * directX[2];
        gridDY[0] = (lenY / gridY) * directY[0];
        gridDY[1] = (lenY / gridY) * directY[1];
        gridDY[2] = (lenY / gridY) * directY[2];

        for (int i = 0; i < gridY; i++) {
            for (int j = 0; j < gridX; j++) {
                gridSample[i][j][0] = corners[0][0] + j*gridDX[0] + i*gridDY[0];
                gridSample[i][j][1] = corners[0][1] + j*gridDX[1] + i*gridDY[1];
                gridSample[i][j][2] = corners[0][2] + j*gridDX[2] + i*gridDY[2];
            }
        }
    }

    @Override public double[] diffuseEmission()  { return diffuseEmission; }
    @Override public double[] specularEmission() { return specularEmission; }
    @Override public int      sampleCount(int areaSubSamples) { return areaSubSamples; }

    @Override
    public void samplePosition(int k, int rayNum, double[] out) {
        int gridSize = gridX * gridY;
        int[] xy = Sampling.getGridNumber((rayNum + k) % gridSize, gridX, gridY);
        int gx = xy[0], gy = xy[1];

        double jx = Rng.uniform(0.0, 1.0);
        double jy = Rng.uniform(0.0, 1.0);

        out[0] = gridSample[gy][gx][0] + jx * gridDX[0] + jy * gridDY[0];
        out[1] = gridSample[gy][gx][1] + jx * gridDX[1] + jy * gridDY[1];
        out[2] = gridSample[gy][gx][2] + jx * gridDX[2] + jy * gridDY[2];
    }

    @Override
    public int shadowCasterScanLimit(int sceneSize) {
        return sceneSize;
    }
}
