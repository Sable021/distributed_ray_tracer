package com.raytracer.render;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stdout progress reporter. Prints the "Rendering Scene..." banner on render start, the
 * 25 / 50 / 75 % milestones as worker threads cross them, and the total elapsed seconds
 * on render finish — preserving exactly the user-facing output that lived in the
 * {@code Renderer.Progress} inner class pre-Phase-F.
 *
 * <p>{@link #onRowDone} is called from any worker thread; the milestone print fires
 * exactly once each, on whichever thread crosses the threshold first
 * ({@link AtomicInteger#compareAndSet} guards the stage transition).
 */
public final class ProgressReporter implements RenderObserver {

    private long startMs;
    private int totalRows;
    private final AtomicInteger completed = new AtomicInteger(0);
    private final AtomicInteger stage     = new AtomicInteger(0);

    @Override
    public void onRenderStart(int totalRows) {
        this.totalRows = totalRows;
        this.startMs   = System.currentTimeMillis();
        this.completed.set(0);
        this.stage.set(0);
        System.out.println("Rendering Scene... Please be Patient");
    }

    @Override
    public void onRowDone(int row) {
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

    @Override
    public void onRenderDone() {
        long elapsed = (System.currentTimeMillis() - startMs) / 1000;
        System.out.println("Rendering Done. Total: " + elapsed + "s");
    }
}
