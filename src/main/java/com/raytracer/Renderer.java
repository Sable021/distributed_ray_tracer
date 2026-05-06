package com.raytracer;

import com.raytracer.geom.Plane;
import com.raytracer.shading.AreaLight;
import com.raytracer.shading.Light;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

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
 * bottom of the image. The {@link com.raytracer.io.ImageWriter ImageWriter} flips on
 * output, and {@link Display} inverts the row index when uploading to the JavaFX
 * {@code WritableImage}.
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
    private final RenderConfig renderConfig;
    private final boolean acesTonemap;
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
     * @param scene        pre-initialised scene to render
     * @param mode         {@link Mode#SUPERSAMPLED} or {@link Mode#DEPTH_OF_FIELD}
     * @param gridX        supersample (and DoF lens) grid width — total rays per pixel = gridX*gridY
     * @param gridY        supersample (and DoF lens) grid height
     * @param maxDepth     maximum recursion depth for reflected/refracted rays
     * @param width        image width in pixels
     * @param height       image height in pixels
     * @param camera       camera and screen-plane configuration
     * @param renderConfig algorithm constants (ambient, shadow samples, etc.)
     */
    public Renderer(Scene scene, Mode mode, int gridX, int gridY, int maxDepth,
                    int width, int height, CameraConfig camera, RenderConfig renderConfig) {
        this.scene  = scene;
        this.mode   = mode;
        this.gridX  = gridX;
        this.gridY  = gridY;
        this.maxDepth = maxDepth;
        this.width  = width;
        this.height = height;
        this.renderConfig  = renderConfig;
        this.acesTonemap   = renderConfig.acesTonemap();
        this.eye          = camera.eye().clone();
        this.scrWxl       = camera.scrWxl();
        this.scrWxr       = camera.scrWxr();
        this.scrHyb       = camera.scrHyb();
        this.scrHyt       = camera.scrHyt();
        this.scrZ         = camera.scrZ();
        this.dofLensWidth = camera.dofLensWidth();
        this.dofLensHeight= camera.dofLensHeight();
        this.dofFocalDist = camera.dofFocalDist();
        this.rayTracer = new RayTracer(scene, maxDepth, gridX, gridY, renderConfig);

        // Initialise light grids for area lights (must happen before any rayTrace call)
        for (Light light : this.scene.lights) {
            if (light instanceof AreaLight area) {
                area.buildGrid(gridX, gridY);
            }
        }
    }

    /** Convenience overload using default algorithm constants. */
    public Renderer(Scene scene, Mode mode, int gridX, int gridY, int maxDepth,
                    int width, int height, CameraConfig camera) {
        this(scene, mode, gridX, gridY, maxDepth, width, height, camera, RenderConfig.defaults());
    }

    /** Convenience overload using default camera and default algorithm constants. */
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
        Plane focalPlane = new Plane(new double[]{0.0, 0.0, -1.0}, dofFocalDist);

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

                double tFocus = focalPlane.intersect(dofRay);
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

    /** Pack an RGB triple into a 0xFFRRGGBB int, applying ACES tone mapping if enabled. */
    private int packArgb(double[] c) {
        double r = acesTonemap ? acesFilm(c[0]) : c[0];
        double g = acesTonemap ? acesFilm(c[1]) : c[1];
        double b = acesTonemap ? acesFilm(c[2]) : c[2];
        return 0xFF000000 | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
    }

    /** Narkowicz 2015 ACES filmic approximation — maps [0,∞) to [0,1]. */
    private static double acesFilm(double x) {
        return Math.max(0.0, Math.min(1.0, (x * (2.51*x + 0.03)) / (x * (2.43*x + 0.59) + 0.14)));
    }

    /** Clamp a colour component to the byte range [0, 255]. */
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
