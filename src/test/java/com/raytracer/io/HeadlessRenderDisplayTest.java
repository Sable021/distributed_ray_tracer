package com.raytracer.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class HeadlessRenderDisplayTest {

    private static String captureStdout(Runnable body) {
        PrintStream original = System.out;
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        System.setOut(new PrintStream(sink));
        try { body.run(); } finally { System.setOut(original); }
        return sink.toString();
    }

    private static String captureStderr(Runnable body) {
        PrintStream original = System.err;
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        System.setErr(new PrintStream(sink));
        try { body.run(); } finally { System.setErr(original); }
        return sink.toString();
    }

    @Test
    void onStartAndOnRowCompleteAreSilent(@TempDir Path tmp) {
        HeadlessRenderDisplay d = new HeadlessRenderDisplay();
        String out = captureStdout(() -> {
            d.onStart(800, 600);
            d.onRowComplete(0, new int[]{0xFF000000}, 1);
            d.onRowComplete(599, new int[]{0xFFFFFFFF}, 1);
        });
        assertEquals("", out, "headless display must not print during render");
    }

    @Test
    void onFinishPrintsTheAbsoluteOutputPath(@TempDir Path tmp) {
        HeadlessRenderDisplay d = new HeadlessRenderDisplay();
        Path out = tmp.resolve("render.ppm");
        String stdout = captureStdout(() -> d.onFinish(new int[0], Duration.ofSeconds(3), out));
        assertTrue(stdout.startsWith("Wrote "), "expected 'Wrote ...' line, got: " + stdout);
        assertTrue(stdout.contains(out.toAbsolutePath().toString()),
                "expected absolute path in output: " + stdout);
    }

    @Test
    void onErrorPrintsTheStackTraceToStderr() {
        HeadlessRenderDisplay d = new HeadlessRenderDisplay();
        RuntimeException boom = new RuntimeException("boom");
        String stderr = captureStderr(() -> d.onError(boom));
        assertTrue(stderr.contains("RuntimeException"), "expected exception class in stderr: " + stderr);
        assertTrue(stderr.contains("boom"), "expected exception message in stderr: " + stderr);
    }
}
