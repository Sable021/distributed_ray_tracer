package com.raytracer.render;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompositeObserverTest {

    /** Records the order in which callbacks fire across the composite's children. */
    private static final class TaggedObserver implements RenderObserver {
        final String tag;
        final List<String> events;
        TaggedObserver(String tag, List<String> events) { this.tag = tag; this.events = events; }

        @Override public void onPrimary()                  { events.add(tag + ":primary"); }
        @Override public void onShadow()                   { events.add(tag + ":shadow");  }
        @Override public void onReflect()                  { events.add(tag + ":reflect"); }
        @Override public void onRefract()                  { events.add(tag + ":refract"); }
        @Override public void onRenderStart(int totalRows) { events.add(tag + ":start:" + totalRows); }
        @Override public void onRowDone(int row)           { events.add(tag + ":row:" + row); }
        @Override public void onRenderDone()               { events.add(tag + ":done"); }
    }

    @Test
    void everyCallbackBroadcastsToEveryChildInDeclaredOrder() {
        List<String> log = new ArrayList<>();
        TaggedObserver a = new TaggedObserver("a", log);
        TaggedObserver b = new TaggedObserver("b", log);
        CompositeObserver c = new CompositeObserver(a, b);

        c.onRenderStart(10);
        c.onPrimary();
        c.onShadow();
        c.onReflect();
        c.onRefract();
        c.onRowDone(3);
        c.onRenderDone();

        assertEquals(List.of(
                "a:start:10", "b:start:10",
                "a:primary", "b:primary",
                "a:shadow",  "b:shadow",
                "a:reflect", "b:reflect",
                "a:refract", "b:refract",
                "a:row:3",   "b:row:3",
                "a:done",    "b:done"
        ), log);
    }

    @Test
    void compositeWithNoChildrenIsSafeNoOp() {
        CompositeObserver c = new CompositeObserver();
        // Must not throw.
        c.onRenderStart(1);
        c.onPrimary(); c.onShadow(); c.onReflect(); c.onRefract();
        c.onRowDone(0);
        c.onRenderDone();
    }

    @Test
    void rayCounterEmbeddedInCompositeStillCounts() {
        RayCounter counter = new RayCounter();
        // Anonymous extra child so the composite wraps more than just the counter.
        RenderObserver noop = new RenderObserver() {};
        CompositeObserver c = new CompositeObserver(counter, noop);

        c.onPrimary(); c.onPrimary();
        c.onShadow();
        c.onReflect(); c.onReflect(); c.onReflect();

        RayCounts s = counter.snapshot();
        assertEquals(2, s.primary());
        assertEquals(1, s.shadow());
        assertEquals(3, s.reflect());
        assertEquals(0, s.refract());
    }
}
