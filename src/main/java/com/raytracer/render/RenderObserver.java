package com.raytracer.render;

/**
 * Cross-cutting render-progress and ray-event sink. Implementations subscribe to the
 * events they care about by overriding the matching default-no-op method, so a single
 * {@code RenderObserver} field on {@link com.raytracer.RayTracer RayTracer} and
 * {@link com.raytracer.Renderer Renderer} replaces what was previously hard-wired
 * {@code AtomicLong} counters and an inner {@code Progress} helper.
 *
 * <p>All callbacks may be invoked from arbitrary worker threads (the renderer is
 * parallel by row), so implementations must be safe under concurrent access. The default
 * no-op bodies make the interface a true mix-in: an observer that only cares about
 * milestones can implement {@code onRowDone} and {@code onRenderDone} alone.
 *
 * <p>Use {@link CompositeObserver} to attach more than one observer to a single render.
 */
public interface RenderObserver {

    /** Called once per primary (camera) ray spawned. */
    default void onPrimary() {}

    /** Called once per shadow ray spawned. */
    default void onShadow() {}

    /** Called once per reflection ray spawned (specular bounce). */
    default void onReflect() {}

    /** Called once per refraction ray spawned (transmission or TIR bounce). */
    default void onRefract() {}

    /** Called once at the start of a render, before any worker thread fires. */
    default void onRenderStart(int totalRows) {}

    /**
     * Called exactly once per completed scanline, on whichever worker thread finishes the
     * row. Implementations must not block — long handlers stall the parallel loop.
     */
    default void onRowDone(int row) {}

    /** Called once after the last row completes, before {@link com.raytracer.Renderer#render} returns. */
    default void onRenderDone() {}
}
