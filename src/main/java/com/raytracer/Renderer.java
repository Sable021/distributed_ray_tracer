package com.raytracer;

import static com.raytracer.SceneObject.ObjectType.*;

/**
 * Top-level pixel loop. Produces an ARGB int buffer in bottom-up row order
 * (row 0 = bottom of screen). PpmIO.write handles the Y-flip to top-down PPM output.
 */
public final class Renderer {

    public enum Mode { SUPERSAMPLED, DEPTH_OF_FIELD }

    // ---- Image / camera constants (exact C++ values) ----
    public static final int SCR_WIDTH  = 1024;
    public static final int SCR_HEIGHT = 768;

    private static final double[] EYE = { -0.3, 3.0, 11.0 };

    /** Screen plane in world-space: left/right x, bottom/top y, at z = SCR_Z. */
    private static final double SCR_WXL = -3.0;
    private static final double SCR_WXR =  3.0;
    private static final double SCR_HYB =  1.25;
    private static final double SCR_HYT =  5.75;
    private static final double SCR_Z   =  8.2;

    // ---- Depth of field (lens size + focal plane) ----
    private static final double DOF_LENS_WIDTH  = 0.4;
    private static final double DOF_LENS_HEIGHT = 0.4;
    private static final double DOF_FOCAL_DIST  = 3.6;

    private final Scene scene;
    private final RayTracer rayTracer;
    private final Mode mode;
    private final int gridX, gridY;
    private final int width, height;
    private final int maxDepth;

    public Renderer(Scene scene, Mode mode, int gridX, int gridY, int maxDepth) {
        this.scene = scene;
        this.mode = mode;
        this.gridX = gridX;
        this.gridY = gridY;
        this.maxDepth = maxDepth;
        this.width = SCR_WIDTH;
        this.height = SCR_HEIGHT;
        this.rayTracer = new RayTracer(scene, maxDepth, gridX, gridY);

        // Initialise light grids for area lights (must happen before any rayTrace call)
        for (int i = 0; i < scene.numActive; i++) {
            SceneObject o = scene.objects[i];
            if (o.isLight && o.type == PLANE) {
                Sampling.createLightGrid(gridX, gridY, o);
            }
        }
    }

    public int getWidth()  { return width; }
    public int getHeight() { return height; }

    public int[] render() {
        return switch (mode) {
            case SUPERSAMPLED    -> renderSupersampled();
            case DEPTH_OF_FIELD  -> renderDepthOfField();
        };
    }

    // -------------------------------------------------------------------------
    // Supersampled render (jittered NxN rays per pixel)
    // -------------------------------------------------------------------------

    private int[] renderSupersampled() {
        int[] pixels = new int[width * height];
        double scrDX = (SCR_WXR - SCR_WXL) / width;
        double scrDY = (SCR_HYT - SCR_HYB) / height;
        int gridSize = gridX * gridY;

        Progress prog = new Progress(height);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                double scrX = SCR_WXL + j * scrDX;
                double scrY = SCR_HYB + i * scrDY;

                double[] acc = new double[3];

                for (int k = 0; k < gridY; k++) {
                    for (int l = 0; l < gridX; l++) {
                        double[] sub = {
                            scrX + (scrDX / gridX) * l + Rng.uniform(0.0, scrDX / gridX),
                            scrY + (scrDY / gridY) * k + Rng.uniform(0.0, scrDY / gridY),
                            SCR_Z
                        };
                        double[] dir = VecMath.direction(EYE, sub);
                        VecMath.normalize(dir);
                        Ray ray = Ray.make(EYE, dir);

                        double[] c = new double[3];
                        rayTracer.rayTrace(ray, 1, 1.0, c, false, k * gridX + l);
                        acc[0] += c[0]; acc[1] += c[1]; acc[2] += c[2];
                    }
                }

                acc[0] /= gridSize;
                acc[1] /= gridSize;
                acc[2] /= gridSize;

                pixels[i * width + j] = packArgb(acc);
            }
            prog.tick(i);
        }
        prog.done();
        return pixels;
    }

    // -------------------------------------------------------------------------
    // Depth of field render (lens-jittered rays toward a focal point)
    // -------------------------------------------------------------------------

    private int[] renderDepthOfField() {
        int[] pixels = new int[width * height];
        double scrDX = (SCR_WXR - SCR_WXL) / width;
        double scrDY = (SCR_HYT - SCR_HYB) / height;
        int gridSize = gridX * gridY;

        double dofDX = DOF_LENS_WIDTH / gridX;
        double dofDY = DOF_LENS_HEIGHT / gridY;

        // Build a synthetic focal plane at z = SCR_Z - DOF_FOCAL_DIST (normal (0,0,-1))
        SceneObject focalPlane = new SceneObject();
        focalPlane.type = PLANE;
        VecMath.set(focalPlane.vectors[0], 0.0, 0.0, -1.0);
        focalPlane.dist = DOF_FOCAL_DIST;

        Progress prog = new Progress(height);

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                double scrX = SCR_WXL + j * scrDX;
                double scrY = SCR_HYB + i * scrDY;

                double[] pixelPt = { scrX, scrY, SCR_Z };

                // Primary ray from eye through the pixel to find the focus point on the focal plane
                double[] dofDir = VecMath.direction(EYE, pixelPt);
                VecMath.normalize(dofDir);
                Ray dofRay = Ray.make(pixelPt, dofDir);

                double tFocus = Intersect.rayPlaneIntersect(dofRay, focalPlane);
                if (tFocus <= 0.0) {
                    // eye ray missed focal plane — skip this pixel
                    pixels[i * width + j] = 0xFF000000;
                    continue;
                }

                double[] focusPt = new double[3];
                VecMath.pointOnLine(focusPt, dofRay.point, dofRay.direct, tFocus);

                // Lens origin: bottom-left of the square aperture centred on the pixel
                double lensOriginX = scrX - DOF_LENS_WIDTH  / 2.0;
                double lensOriginY = scrY - DOF_LENS_HEIGHT / 2.0;

                double[] acc = new double[3];

                for (int k = 0; k < gridY; k++) {
                    for (int l = 0; l < gridX; l++) {
                        double[] lensPt = {
                            lensOriginX + dofDX * l + Rng.uniform(0.0, dofDX),
                            lensOriginY + dofDY * k + Rng.uniform(0.0, dofDY),
                            SCR_Z
                        };
                        double[] dir = VecMath.direction(lensPt, focusPt);
                        VecMath.normalize(dir);
                        Ray ray = Ray.make(lensPt, dir);

                        double[] c = new double[3];
                        rayTracer.rayTrace(ray, 1, 1.0, c, false, k * gridX + l);
                        acc[0] += c[0]; acc[1] += c[1]; acc[2] += c[2];
                    }
                }

                acc[0] /= gridSize;
                acc[1] /= gridSize;
                acc[2] /= gridSize;
                pixels[i * width + j] = packArgb(acc);
            }
            prog.tick(i);
        }
        prog.done();
        return pixels;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static int packArgb(double[] c) {
        int r = clamp255(c[0]);
        int g = clamp255(c[1]);
        int b = clamp255(c[2]);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int clamp255(double v) {
        if (v <= 0.0) return 0;
        if (v >= 1.0) return 255;
        return (int)(v * 255.0);
    }

    /** Prints 25/50/75% progress lines matching the C++ indicator logic. */
    private static final class Progress {
        private final int totalRows;
        private final long startMs = System.currentTimeMillis();
        private int stage = 0;

        Progress(int totalRows) {
            this.totalRows = totalRows;
            System.out.println("Rendering Scene... Please be Patient");
        }

        void tick(int row) {
            int threshold = switch (stage) {
                case 0 -> totalRows / 4;
                case 1 -> totalRows / 2;
                case 2 -> (3 * totalRows) / 4;
                default -> Integer.MAX_VALUE;
            };
            if (row >= threshold) {
                long elapsed = (System.currentTimeMillis() - startMs) / 1000;
                String[] tag = { "25%", "50%", "75%" };
                System.out.printf("(%s completed) elapsed=%ds%n", tag[stage], elapsed);
                stage++;
            }
        }

        void done() {
            long elapsed = (System.currentTimeMillis() - startMs) / 1000;
            System.out.println("Rendering Done. Total: " + elapsed + "s");
        }
    }
}
