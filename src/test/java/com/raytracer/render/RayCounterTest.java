package com.raytracer.render;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class RayCounterTest {

    @Test
    void freshCounterReportsAllZero() {
        RayCounter c = new RayCounter();
        RayCounts s = c.snapshot();
        assertEquals(0, s.primary());
        assertEquals(0, s.shadow());
        assertEquals(0, s.reflect());
        assertEquals(0, s.refract());
        assertEquals(0, s.total());
    }

    @Test
    void eachCallbackIncrementsItsCounter() {
        RayCounter c = new RayCounter();
        c.onPrimary(); c.onPrimary(); c.onPrimary();
        c.onShadow(); c.onShadow();
        c.onReflect();
        // no onRefract calls

        RayCounts s = c.snapshot();
        assertEquals(3, s.primary());
        assertEquals(2, s.shadow());
        assertEquals(1, s.reflect());
        assertEquals(0, s.refract());
        assertEquals(6, s.total());
    }

    /**
     * The renderer fires every callback from arbitrary worker threads, so the counter must
     * record every call without races. 16 threads * 10000 increments per type — losing any
     * one would fail the assert.
     */
    @Test
    void callbacksAreThreadSafeUnderConcurrency() throws InterruptedException {
        RayCounter c = new RayCounter();
        int threads = 16;
        int perThread = 10_000;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < threads; t++) {
                pool.submit(() -> {
                    for (int i = 0; i < perThread; i++) {
                        c.onPrimary();
                        c.onShadow();
                        c.onReflect();
                        c.onRefract();
                    }
                });
            }
        } finally {
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS), "pool did not finish");
        }

        long expected = (long) threads * perThread;
        RayCounts s = c.snapshot();
        assertEquals(expected, s.primary());
        assertEquals(expected, s.shadow());
        assertEquals(expected, s.reflect());
        assertEquals(expected, s.refract());
    }

    @Test
    void otherLifecycleCallbacksDoNotAffectCounters() {
        RayCounter c = new RayCounter();
        c.onRenderStart(100);
        c.onRowDone(0);
        c.onRowDone(99);
        c.onRenderDone();

        RayCounts s = c.snapshot();
        assertEquals(0, s.total(), "lifecycle events must not increment ray counters");
    }
}
