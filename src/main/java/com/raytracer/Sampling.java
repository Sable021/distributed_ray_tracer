package com.raytracer;

/**
 * Stratified-sampling helpers for glossy reflection rays and area-light shadow rays.
 *
 * <p>{@link #getSampleVertex} jitters within a grid orthogonal to the perfect-reflection
 * direction. {@link #getGridNumber} maps a trace number to a 2-D grid cell using a
 * {@code *7} scrambler so adjacent rays sample different cells. (Phase B's
 * {@code AreaLight} owns its own light-grid storage; what remains here is shared between
 * area-light sampling and glossy-reflection sampling. Phase D will fold these into a
 * {@code Sampler} interface.)
 */
public final class Sampling {

    private Sampling() {}

    /** sqrt(0.125) — diagonal offset for the glossy reflection sample grid */
    static final double DIAG_HALF = Math.sqrt(0.125);

    /**
     * Compute one jittered sample point on the glossy reflection grid for the
     * given trace index. Uses stratified (trace_num*7) % gridSize cell selection.
     */
    public static void getSampleVertex(double[] outVertex, int gsizeX, int gsizeY,
                                       double[] N, double[] midpt, int traceNum) {
        double DX = 0.5 / gsizeX;
        double DY = 0.5 / gsizeY;

        double[] V = {0.5, 0.5, 0.5};
        double[] dia1 = new double[3];
        double[] dia2 = new double[3];
        VecMath.cross(dia1, V, N);
        VecMath.cross(dia2, dia1, N);

        double[] gridOrigin = new double[3];
        VecMath.pointOnLine(gridOrigin, midpt, dia1, DIAG_HALF);

        double[] gridX = new double[3];
        VecMath.direction(gridX, dia1, dia2);
        VecMath.normalize(gridX);

        double[] gridY = new double[3];
        VecMath.cross(gridY, gridX, N);

        gridX[0] *= DX; gridX[1] *= DX; gridX[2] *= DX;
        gridY[0] *= DY; gridY[1] *= DY; gridY[2] *= DY;

        double rx = Rng.uniform(0.0, 1.0);
        double ry = Rng.uniform(0.0, 1.0);

        int[] xy = getGridNumber(traceNum, gsizeX, gsizeY);
        int x = xy[0], y = xy[1];

        outVertex[0] = gridOrigin[0] + (x+rx)*gridX[0] + (y+ry)*gridY[0];
        outVertex[1] = gridOrigin[1] + (x+rx)*gridX[1] + (y+ry)*gridY[1];
        outVertex[2] = gridOrigin[2] + (x+rx)*gridX[2] + (y+ry)*gridY[2];
    }

    /**
     * Map a trace number to a 2-D grid cell (x, y) using a *7 scrambler so
     * adjacent rays sample different cells.
     * Returns int[]{x, y}.
     */
    public static int[] getGridNumber(int traceNum, int gsizeX, int gsizeY) {
        int gridSize = gsizeX * gsizeY;
        int gridNum  = (traceNum * 7) % gridSize;   // preserve the magic 7 multiplier
        // Row-major decomposition: x = idx % cols, y = idx / cols (cols = gsizeX).
        // The C++ original used /gsizeY here; harmless for square grids but wrong otherwise.
        return new int[]{ gridNum % gsizeX, gridNum / gsizeX };
    }
}
