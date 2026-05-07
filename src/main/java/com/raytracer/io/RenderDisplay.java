package com.raytracer.io;

import java.nio.file.Path;
import java.time.Duration;

/**
 * UI / output sink for a single render. Implementations decide whether to draw the
 * progressive image into a window, log to stdout, or stay completely silent — keeping
 * the JavaFX dependency out of {@link com.raytracer.Renderer Renderer} and behind one
 * named seam.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>{@link #onStart} — once, before the first ray fires.</li>
 *   <li>{@link #onRowComplete} — once per scanline, on the render thread; bottom-up row order.</li>
 *   <li>{@link #onFinish} — once, after the file has been written.</li>
 *   <li>{@link #onError} — replaces {@code onFinish} when an exception aborts the render.</li>
 * </ol>
 *
 * <p>{@code onRowComplete} runs on a render worker thread. UI implementations must
 * marshal updates to their toolkit's UI thread internally (e.g. {@code Platform.runLater}
 * for JavaFX); the orchestrator does not do that for them.
 */
public interface RenderDisplay {

    /** Render is about to start. {@code width}/{@code height} are pixels. */
    void onStart(int width, int height);

    /**
     * One scanline finished.
     *
     * @param row    index of the just-completed row (0 = bottom)
     * @param pixels full ARGB pixel buffer (caller must not retain — copy out before returning)
     * @param width  image width in pixels
     */
    void onRowComplete(int row, int[] pixels, int width);

    /**
     * Render and file-write completed successfully.
     *
     * @param pixels     final ARGB pixel buffer
     * @param elapsed    wall-clock time from {@link #onStart} to render completion
     * @param outputPath path the image was written to
     */
    void onFinish(int[] pixels, Duration elapsed, Path outputPath);

    /** Render or file-write failed. {@code onFinish} will not be called. */
    void onError(Throwable t);
}
