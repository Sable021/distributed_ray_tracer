package com.raytracer.io;

import java.nio.file.Path;
import java.time.Duration;

/**
 * No-op {@link RenderDisplay} for the {@code --headless} path. Stays silent during the
 * render itself (the renderer already prints "Rendering Scene..." and the milestone /
 * total lines via {@link com.raytracer.render.ProgressReporter ProgressReporter}); only
 * the final {@code "Wrote <path>"} line and any error is emitted by this display, so the
 * stdout output of a headless run is unchanged from pre-Phase-G.
 */
public final class HeadlessRenderDisplay implements RenderDisplay {

    @Override public void onStart(int width, int height) { /* silent */ }

    @Override public void onRowComplete(int row, int[] pixels, int width) { /* silent */ }

    @Override
    public void onFinish(int[] pixels, Duration elapsed, Path outputPath) {
        System.out.println("Wrote " + outputPath.toAbsolutePath());
    }

    @Override
    public void onError(Throwable t) {
        t.printStackTrace();
    }
}
