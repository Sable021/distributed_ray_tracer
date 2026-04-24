package com.raytracer;

public final class Sampling {

    private Sampling() {}

    static final int MAX_GRID_X = 8;
    static final int MAX_GRID_Y = 8;

    /** sqrt(0.125) — diagonal offset for the glossy reflection sample grid */
    static final double DIAG_HALF = Math.sqrt(0.125);

    // -------------------------------------------------------------------------
    // Area light grid — stored directly on the SceneObject
    // -------------------------------------------------------------------------

    /**
     * Populate obj.lightGridSample with uniformly-spaced grid points across the
     * area light's surface. Corners are in obj.vectors[3..6].
     */
    public static void createLightGrid(int gsizeX, int gsizeY, SceneObject obj) {
        if (!obj.isLight) {
            System.err.println("createLightGrid: object is not a light");
            return;
        }

        double[] directX = new double[3];
        double[] directY = new double[3];
        VecMath.direction(directX, obj.vectors[3], obj.vectors[4]);
        VecMath.direction(directY, obj.vectors[3], obj.vectors[6]);

        double lenX = VecMath.magnitude(directX);
        double lenY = VecMath.magnitude(directY);

        VecMath.normalize(directX);
        VecMath.normalize(directY);

        double[] gridOrigin = new double[3];
        VecMath.copy(obj.vectors[3], gridOrigin);

        double[] gridDX = new double[]{
            (lenX / gsizeX) * directX[0],
            (lenX / gsizeX) * directX[1],
            (lenX / gsizeX) * directX[2]
        };
        double[] gridDY = new double[]{
            (lenY / gsizeY) * directY[0],
            (lenY / gsizeY) * directY[1],
            (lenY / gsizeY) * directY[2]
        };

        VecMath.copy(gridDX, obj.lightGridDX);
        VecMath.copy(gridDY, obj.lightGridDY);
        obj.lightDX = lenX / gsizeX;
        obj.lightDY = lenY / gsizeY;

        for (int i = 0; i < gsizeY; i++) {
            for (int j = 0; j < gsizeX; j++) {
                obj.lightGridSample[i][j][0] = gridOrigin[0] + j*gridDX[0] + i*gridDY[0];
                obj.lightGridSample[i][j][1] = gridOrigin[1] + j*gridDX[1] + i*gridDY[1];
                obj.lightGridSample[i][j][2] = gridOrigin[2] + j*gridDX[2] + i*gridDY[2];
            }
        }
    }

    /** Holder for the four output values of getLightGridDelta */
    public record LightGridDelta(double lightDX, double lightDY,
                                 double[] lightGridDX, double[] lightGridDY) {}

    public static LightGridDelta getLightGridDelta(SceneObject obj) {
        double[] dx = new double[3];
        double[] dy = new double[3];
        VecMath.copy(obj.lightGridDX, dx);
        VecMath.copy(obj.lightGridDY, dy);
        return new LightGridDelta(obj.lightDX, obj.lightDY, dx, dy);
    }

    // -------------------------------------------------------------------------
    // Glossy reflection / refraction sample grid
    // -------------------------------------------------------------------------

    /**
     * Build a 2-D jittered sample grid centred at midpt, oriented perpendicular to N.
     * Grid is a square of side 0.5 (diagonal = DIAG_HALF = sqrt(0.125)).
     * Returns sample_grid[gsizeY][gsizeX][3].
     */
    public static double[][][] createSampleGrid(int gsizeX, int gsizeY, double[] N, double[] midpt) {
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

        double[][][] grid = new double[gsizeY][gsizeX][3];
        for (int i = 0; i < gsizeY; i++) {
            for (int j = 0; j < gsizeX; j++) {
                double rx = Rng.uniform(0.0, DX);
                double ry = Rng.uniform(0.0, DY);
                grid[i][j][0] = gridOrigin[0] + (j+rx)*gridX[0] + (i+ry)*gridY[0];
                grid[i][j][1] = gridOrigin[1] + (j+rx)*gridX[1] + (i+ry)*gridY[1];
                grid[i][j][2] = gridOrigin[2] + (j+rx)*gridX[2] + (i+ry)*gridY[2];
            }
        }
        return grid;
    }

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
        return new int[]{ gridNum % gsizeX, gridNum / gsizeY };
    }
}
