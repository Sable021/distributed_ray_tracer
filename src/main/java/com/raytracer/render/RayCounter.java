package com.raytracer.render;

import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link RenderObserver} that tallies ray events into four {@link AtomicLong} counters,
 * one per ray type. Replaces the inline counters that used to live on
 * {@link com.raytracer.RayTracer RayTracer} so the integrator no longer carries
 * cross-cutting telemetry concerns.
 *
 * <p>{@link #snapshot} returns a stable {@link RayCounts} tuple read from each counter;
 * the four reads are not atomic with respect to one another, but on a render in progress
 * the readings are within microseconds of each other and the call sites that consume
 * them (the JavaFX status label, the final summary) are tolerant of that.
 */
public final class RayCounter implements RenderObserver {

    private final AtomicLong primary = new AtomicLong();
    private final AtomicLong shadow  = new AtomicLong();
    private final AtomicLong reflect = new AtomicLong();
    private final AtomicLong refract = new AtomicLong();

    @Override public void onPrimary() { primary.incrementAndGet(); }
    @Override public void onShadow()  { shadow.incrementAndGet(); }
    @Override public void onReflect() { reflect.incrementAndGet(); }
    @Override public void onRefract() { refract.incrementAndGet(); }

    /** Live (non-atomic across counters) snapshot of every ray type at the call moment. */
    public RayCounts snapshot() {
        return new RayCounts(primary.get(), shadow.get(), reflect.get(), refract.get());
    }
}
