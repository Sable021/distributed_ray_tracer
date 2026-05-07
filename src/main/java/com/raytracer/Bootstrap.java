package com.raytracer;

import com.raytracer.io.ImageWriters;
import com.raytracer.io.RenderDisplay;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Composition-root orchestrator that runs a single render through a configured
 * {@link Renderer} and reports lifecycle events to a {@link RenderDisplay}. Centralises
 * the "render → write file → notify" sequence so the headless ({@link Main}) and JavaFX
 * ({@link Display}) entry points share one code path and stay in sync.
 *
 * <p>This class deliberately knows nothing about argument parsing, scene loading, or UI
 * toolkits — its inputs are a fully-constructed renderer and display. Callers wire those
 * up themselves, then hand them off here.
 */
public final class Bootstrap {

    private Bootstrap() {}

    /**
     * Execute the render, write the output file, and notify the display of every
     * lifecycle event. Synchronous — runs to completion on the calling thread, so the
     * caller is responsible for invoking it on a worker thread when blocking matters
     * (e.g. the JavaFX path uses a daemon platform thread).
     *
     * @param renderer the configured renderer to drive
     * @param display  lifecycle sink — called from this thread
     * @param outPath  where to write the image
     * @param format   image format ({@code "ppm"}, {@code "png"}, {@code "bmp"})
     */
    public static void runRender(Renderer renderer, RenderDisplay display,
                                 Path outPath, String format) {
        display.onStart(renderer.getWidth(), renderer.getHeight());
        renderer.setRowListener(display::onRowComplete);

        try {
            long startMs = System.currentTimeMillis();
            int[] pixels = renderer.render();
            long elapsedMs = System.currentTimeMillis() - startMs;
            Path written = ImageWriters.forFormat(format)
                    .write(outPath, pixels, renderer.getWidth(), renderer.getHeight());
            display.onFinish(pixels, Duration.ofMillis(elapsedMs), written);
        } catch (Exception e) {
            display.onError(e);
        }
    }
}
