package com.raytracer.render;

import java.util.List;

/**
 * Fan-out {@link RenderObserver} that broadcasts every callback to a fixed list of
 * children in declared order. Used by {@link com.raytracer.Renderer Renderer} to combine
 * a {@link RayCounter} and a {@link ProgressReporter} into a single observer reference,
 * and by callers that want to snap an extra listener (e.g. a JavaFX status label) into
 * the same render.
 *
 * <p>Children are stored in an immutable list and invoked sequentially; an exception
 * thrown by one child propagates and skips the rest, matching the fail-fast convention
 * of the rest of the renderer.
 */
public final class CompositeObserver implements RenderObserver {

    private final List<RenderObserver> children;

    /** Build a composite from the given children (in fan-out order). */
    public CompositeObserver(RenderObserver... children) {
        this.children = List.of(children);
    }

    @Override public void onPrimary() { for (RenderObserver c : children) c.onPrimary(); }
    @Override public void onShadow()  { for (RenderObserver c : children) c.onShadow(); }
    @Override public void onReflect() { for (RenderObserver c : children) c.onReflect(); }
    @Override public void onRefract() { for (RenderObserver c : children) c.onRefract(); }

    @Override
    public void onRenderStart(int totalRows) {
        for (RenderObserver c : children) c.onRenderStart(totalRows);
    }

    @Override
    public void onRowDone(int row) {
        for (RenderObserver c : children) c.onRowDone(row);
    }

    @Override
    public void onRenderDone() {
        for (RenderObserver c : children) c.onRenderDone();
    }
}
