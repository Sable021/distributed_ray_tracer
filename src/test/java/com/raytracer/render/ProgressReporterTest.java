package com.raytracer.render;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class ProgressReporterTest {

    /**
     * Replace stdout for a closure so we can inspect what the reporter printed without
     * polluting the test runner's console output.
     */
    private static String captureStdout(Runnable body) {
        PrintStream original = System.out;
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        System.setOut(new PrintStream(sink));
        try {
            body.run();
        } finally {
            System.setOut(original);
        }
        return sink.toString();
    }

    @Test
    void onRenderStartPrintsTheBanner() {
        ProgressReporter p = new ProgressReporter();
        String out = captureStdout(() -> p.onRenderStart(100));
        assertTrue(out.contains("Rendering Scene"), "banner missing: " + out);
    }

    @Test
    void milestonesFireOnceEachAtCorrectThresholds() {
        ProgressReporter p = new ProgressReporter();
        String out = captureStdout(() -> {
            p.onRenderStart(100);
            for (int row = 0; row < 100; row++) {
                p.onRowDone(row);
            }
            p.onRenderDone();
        });

        // Each tag must appear exactly once.
        assertEquals(1, countOccurrences(out, "25%"), out);
        assertEquals(1, countOccurrences(out, "50%"), out);
        assertEquals(1, countOccurrences(out, "75%"), out);
        // Final summary
        assertTrue(out.contains("Rendering Done"), "missing final summary: " + out);
    }

    /**
     * Even with thousands of extra row notifications past the last milestone, the
     * 25/50/75 % prints fire exactly once each — the {@code stage} CAS guarantees that.
     */
    @Test
    void milestonesAreNotRefiredIfMoreRowsArriveThanTotalRows() {
        ProgressReporter p = new ProgressReporter();
        String out = captureStdout(() -> {
            p.onRenderStart(40);
            for (int row = 0; row < 4000; row++) {
                p.onRowDone(row);
            }
        });

        assertEquals(1, countOccurrences(out, "25%"));
        assertEquals(1, countOccurrences(out, "50%"));
        assertEquals(1, countOccurrences(out, "75%"));
    }

    @Test
    void rayEventCallbacksAreNoOps() {
        ProgressReporter p = new ProgressReporter();
        String out = captureStdout(() -> {
            p.onRenderStart(10);
            p.onPrimary(); p.onShadow(); p.onReflect(); p.onRefract();
        });
        // After onRenderStart, only the banner should have printed.
        assertEquals(1, countOccurrences(out, "Rendering Scene"));
        assertFalse(out.contains("25%"));
    }

    @Test
    void onRenderStartResetsStateForReuse() {
        ProgressReporter p = new ProgressReporter();
        String firstRun = captureStdout(() -> {
            p.onRenderStart(40);
            for (int r = 0; r < 40; r++) p.onRowDone(r);
        });
        assertEquals(1, countOccurrences(firstRun, "25%"));

        String secondRun = captureStdout(() -> {
            p.onRenderStart(40);
            for (int r = 0; r < 40; r++) p.onRowDone(r);
        });
        assertEquals(1, countOccurrences(secondRun, "25%"),
                "milestone must fire again on the second render");
    }

    private static int countOccurrences(String haystack, String needle) {
        int n = 0, idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) { n++; idx += needle.length(); }
        return n;
    }
}
