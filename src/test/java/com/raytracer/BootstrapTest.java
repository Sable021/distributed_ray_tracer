package com.raytracer;

import com.raytracer.io.RenderDisplay;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives {@link Bootstrap#runRender} end-to-end through a tiny renderer + recording
 * {@link RenderDisplay} fake to confirm the orchestration contract: lifecycle methods
 * fire in the expected order, the file lands on disk, and exceptions route to
 * {@code onError} instead of escaping.
 */
class BootstrapTest {

    /** Recording display — captures every callback in order with a tiny string log. */
    private static final class RecordingDisplay implements RenderDisplay {
        final List<String> events = new ArrayList<>();
        Path lastOutput;
        int rowsSeen;
        Throwable error;

        @Override public void onStart(int width, int height) {
            events.add("start:" + width + "x" + height);
        }
        @Override public void onRowComplete(int row, int[] pixels, int width) {
            rowsSeen++;
        }
        @Override public void onFinish(int[] pixels, Duration elapsed, Path outputPath) {
            events.add("finish:" + outputPath.getFileName());
            lastOutput = outputPath;
        }
        @Override public void onError(Throwable t) {
            events.add("error:" + t.getClass().getSimpleName());
            error = t;
        }
    }

    private static String captureStdout(Runnable body) {
        PrintStream original = System.out;
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        System.setOut(new PrintStream(sink));
        try { body.run(); } finally { System.setOut(original); }
        return sink.toString();
    }

    private static Renderer tinyRenderer() {
        // 16x12 quick render with the built-in scene; ~milliseconds.
        Scene scene = Scene.initialise();
        return new Renderer(scene, Renderer.Mode.SUPERSAMPLED, 1, 1, 2, 16, 12);
    }

    @Test
    void runRenderFiresLifecycleInOrderAndWritesTheFile(@TempDir Path tmp) {
        Renderer renderer = tinyRenderer();
        RecordingDisplay display = new RecordingDisplay();
        Path out = tmp.resolve("render.ppm");

        captureStdout(() -> Bootstrap.runRender(renderer, display, out, "ppm"));

        assertEquals(2, display.events.size(), "expected start + finish: " + display.events);
        assertEquals("start:16x12", display.events.get(0));
        assertTrue(display.events.get(1).startsWith("finish:"));
        assertEquals(12, display.rowsSeen, "every scanline must be reported");
        assertEquals(out, display.lastOutput);
        assertTrue(Files.exists(out), "output file must be written: " + out);
    }

    @Test
    void runRenderRoutesIoExceptionsToOnError(@TempDir Path tmp) {
        Renderer renderer = tinyRenderer();
        RecordingDisplay display = new RecordingDisplay();
        // Path inside a non-existent directory — write fails with FileNotFoundException.
        Path out = tmp.resolve("does-not-exist").resolve("render.ppm");

        captureStdout(() -> Bootstrap.runRender(renderer, display, out, "ppm"));

        assertNotNull(display.error, "exception must be routed to onError");
        assertFalse(display.events.contains("finish:render.ppm"),
                "onFinish must not fire when the write fails");
        assertEquals(12, display.rowsSeen,
                "render itself succeeds — only the file write fails");
    }

    @Test
    void runRenderRoutesUnknownFormatToOnError(@TempDir Path tmp) {
        Renderer renderer = tinyRenderer();
        RecordingDisplay display = new RecordingDisplay();
        Path out = tmp.resolve("render.xyz");

        captureStdout(() -> Bootstrap.runRender(renderer, display, out, "xyz"));

        assertNotNull(display.error);
        assertTrue(display.error instanceof IllegalArgumentException,
                "expected IllegalArgumentException from ImageWriters.forFormat: "
                        + display.error.getClass());
    }
}
