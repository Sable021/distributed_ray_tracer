package com.raytracer;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static com.raytracer.SceneObject.ObjectType.*;

/**
 * Top-level pixel loop, owning the camera + screen-plane geometry and dispatching to
 * the appropriate render mode.
 *
 * <p>For each pixel, fires {@code gridX * gridY} jittered primary rays through
 * {@link RayTracer#rayTrace} and averages the resulting colours (supersampling). The
 * depth-of-field mode additionally jitters the ray origin across a small lens
 * aperture aimed at a focal plane, producing the bokeh effect.
 *
 * <p>The output buffer is row-major ARGB ints in <b>bottom-up</b> order — row 0 is the
 * bottom of the image. {@link PpmIO#write} flips on output, and {@link Display}
 * inverts the row index when uploading to the JavaFX {@code WritableImage}.
 */
public final class Renderer {

    /** Render strategy selector. */
    public enum Mode {
        /** Standard supersampled render: jittered NxN rays per pixel from the eye through the screen plane. */
        SUPERSAMPLED,
        /** Depth-of-field render: jitter ray origin across a lens aperture aimed at a focal plane. */
        DEPTH_OF_FIELD
    }

    /**
     * Optional callback invoked after each fully-rendered scanline. Used by
     * {@link Display} to upload completed rows to the JavaFX {@code WritableImage} for
     * progressive display.
     */
    @FunctionalInterface
    public interface RowListener {
        /**
         * @param row    index of the just-completed row (0 = bottom of image)
         * @param pixels the full pixel buffer (caller must not retain a reference;
         *               copy out the row of interest before returning)
         * @param width  image width in pixels
         */
        void onRowComplete(int row, int[] pixels, int width);
    }

    // ---- Image size defaults ----
    /** Default image width in pixels. Used when no {@code --width=N} flag is supplied. */
    public static final int DEFAULT_WIDTH  = 1440;
    /** Default image height in pixels. Used when no {@code --height=N} flag is supplied. */
    public static final int DEFAULT_HEIGHT = 1080;

    // ---- Camera / screen-plane / DoF (injected via CameraConfig) ----
    private final double[] eye;
    private final double scrWxl, scrWxr, scrHyb, scrHyt, scrZ;
    private final double dofLensWidth, dofLensHeight, dofFocalDist;

    private final Scene scene;
    private final RayTracer rayTracer;
    private final Mode mode;
    private final int gridX, gridY;
    private final int width, height;
    private final int maxDepth;
    private RowListener rowListener;

    /**
     * Register a callback invoked after each scanline completes. Pass {@code null} to
     * remove the existing listener. Listeners run on the render thread, so cheap work
     * only — long handlers will stall the inner loop.
     */
    public void setRowListener(RowListener listener) {
        this.rowListener = listener;
    }

    /**
     * @param scene    pre-initialised scene to render
     * @param mode     {@link Mode#SUPERSAMPLED} or {@link Mode#DEPTH_OF_FIELD}
     * @param gridX    supersample (and DoF lens) grid width — total rays per pixel = gridX*gridY
     * @param gridY    supersample (and DoF lens) grid height
     * @param maxDepth maximum recursion depth for reflected/refracted rays
     * @param width    image width in pixels
     * @param height   image height in pixels
     * @param camera   camera and screen-plane configuration
     */
    public Renderer(Scene scene, Mode mode, int gridX, int gridY, int maxDepth,
                    int width, int height, CameraConfig camera) {
        this.scene  = scene;
        this.mode   = mode;
        this.gridX  = gridX;
        this.gridY  = gridY;
        this.maxDepth = maxDepth;
        this.width  = width;
        this.height = height;
        this.eye          = camera.eye().clone();
        this.scrWxl       = camera.scrWxl();
        this.scrWxr       = camera.scrWxr();
        this.scrHyb       = camera.scrHyb();
        this.scrHyt       = camera.scrHyt();
        this.scrZ         = camera.scrZ();
        this.dofLensWidth = camera.dofLensWidth();
        this.dofLensHeight= camera.dofLensHeight();
        this.dofFocalDist = camera.dofFocalDist();
        this.rayTracer = new RayTracer(scene, maxDepth, gridX, gridY);

        // Initialise light grids for area lights (must happen before any rayTrace call)
        for (int i = 0; i < this.scene.numActive; i++) {
            SceneObject o = scene.objects[i];
            if (o.isLight && o.type == PLANE) {
                Sampling.createLightGrid(gridX, gridY, o);
            }
        }
    }

    /** Convenience overload using the default hardcoded camera (C++ scene parity). */
    public Renderer(Scene scene, Mode mode, int gridX, int gridY, int maxDepth, int width, int height) {
        this(scene, mode, gridX, gridY, maxDepth, width, height, CameraConfig.defaults());
    }

    /** @return image width in pixels. */
    public int getWidth()  { return width; }
    /** @return image height in pixels. */
    public int getHeight() { return height; }
    /** @return live snapshot of rays cast so far, broken down by type. */
    public RayTracer.RayCounts getRayCounts() { return rayTracer.getRayCounts(); }

    /**
     * Run the render. Synchronous: returns only after every pixel has been shaded.
     * Dispatches to the appropriate strategy based on the configured {@link Mode}.
     *
     * @return ARGB pixel buffer in bottom-up row order; length {@code width * height}
     */
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
        double scrDX = (scrWxr - scrWxl) / width;
        double scrDY = (scrHyt - scrHyb) / height;
        int gridSize = gridX * gridY;

        Progress prog = new Progress(height);

        IntStream.range(0, height).parallel().forEach(i -> {
            Rng.reseed(rowSeed(i));
            for (int j = 0; j < width; j++) {
                double scrX = scrWxl + j * scrDX;
                double scrY = scrHyb + i * scrDY;

                double[] acc = new double[3];

                for (int k = 0; k < gridY; k++) {
                    for (int l = 0; l < gridX; l++) {
                        double[] sub = {
                            scrX + (scrDX / gridX) * l + Rng.uniform(0.0, scrDX / gridX),
                            scrY + (scrDY / gridY) * k + Rng.uniform(0.0, scrDY / gridY),
                            scrZ
                        };
                        double[] dir = VecMath.direction(eye, sub);
                        VecMath.normalize(dir);
                        Ray ray = Ray.make(eye, dir);

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
            prog.tick();
            if (rowListener != null) rowListener.onRowComplete(i, pixels, width);
        });
        prog.done();
        return pixels;
    }

    // -------------------------------------------------------------------------
    // Depth of field render (lens-jittered rays toward a focal point)
    // -------------------------------------------------------------------------

    private int[] renderDepthOfField() {
        int[] pixels = new int[width * height];
        double scrDX = (scrWxr - scrWxl) / width;
        double scrDY = (scrHyt - scrHyb) / height;
        int gridSize = gridX * gridY;

        double dofDX = dofLensWidth  / gridX;
        double dofDY = dofLensHeight / gridY;

        // Build a synthetic focal plane at z = scrZ - dofFocalDist (normal (0,0,-1))
        SceneObject focalPlane = new SceneObject();
        focalPlane.type = PLANE;
        VecMath.set(focalPlane.vectors[0], 0.0, 0.0, -1.0);
        focalPlane.dist = dofFocalDist;

        Progress prog = new Progress(height);

        IntStream.range(0, height).parallel().forEach(i -> {
            Rng.reseed(rowSeed(i));
            for (int j = 0; j < width; j++) {
                double scrX = scrWxl + j * scrDX;
                double scrY = scrHyb + i * scrDY;

                double[] pixelPt = { scrX, scrY, scrZ };

                // Primary ray from eye through the pixel to find the focus point on the focal plane
                double[] dofDir = VecMath.direction(eye, pixelPt);
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
                double lensOriginX = scrX - dofLensWidth  / 2.0;
                double lensOriginY = scrY - dofLensHeight / 2.0;

                double[] acc = new double[3];

                for (int k = 0; k < gridY; k++) {
                    for (int l = 0; l < gridX; l++) {
                        double[] lensPt = {
                            lensOriginX + dofDX * l + Rng.uniform(0.0, dofDX),
                            lensOriginY + dofDY * k + Rng.uniform(0.0, dofDY),
                            scrZ
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
            prog.tick();
            if (rowListener != null) rowListener.onRowComplete(i, pixels, width);
        });
        prog.done();
        return pixels;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Pack an RGB triple in [0,1] into a 0xFFRRGGBB int, clamping each channel. */
    private static int packArgb(double[] c) {
        int r = clamp255(c[0]);
        int g = clamp255(c[1]);
        int b = clamp255(c[2]);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Clamp a normalised colour component {@code v ∈ [0, 1]} into the byte range [0, 255]. */
    private static int clamp255(double v) {
        if (v <= 0.0) return 0;
        if (v >= 1.0) return 255;
        return (int)(v * 255.0);
    }

    /**
     * Per-row deterministic RNG seed. Mixed with a large prime so adjacent rows produce
     * well-separated streams even though seed inputs are sequential.
     */
    private static long rowSeed(int row) {
        return 0x9E3779B97F4A7C15L * (row + 1L);
    }

    /**
     * Thread-safe progress reporter. {@link #tick} is invoked once per completed row from
     * any thread; the 25/50/75% milestones fire exactly once each, on whichever thread
     * crosses the threshold first.
     */
    private static final class Progress {
        private final int totalRows;
        private final long startMs = System.currentTimeMillis();
        private final AtomicInteger completed = new AtomicInteger(0);
        private final AtomicInteger stage = new AtomicInteger(0);

        Progress(int totalRows) {
            this.totalRows = totalRows;
            System.out.println("Rendering Scene... Please be Patient");
        }

        void tick() {
            int done = completed.incrementAndGet();
            int s = stage.get();
            int threshold = switch (s) {
                case 0 -> totalRows / 4;
                case 1 -> totalRows / 2;
                case 2 -> (3 * totalRows) / 4;
                default -> Integer.MAX_VALUE;
            };
            if (done >= threshold && stage.compareAndSet(s, s + 1)) {
                long elapsed = (System.currentTimeMillis() - startMs) / 1000;
                String[] tag = { "25%", "50%", "75%" };
                System.out.printf("(%s completed) elapsed=%ds%n", tag[s], elapsed);
            }
        }

        void done() {
            long elapsed = (System.currentTimeMillis() - startMs) / 1000;
            System.out.println("Rendering Done. Total: " + elapsed + "s");
        }
    }
}
