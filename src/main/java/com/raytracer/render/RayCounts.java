package com.raytracer.render;

/**
 * Snapshot of rays cast so far in a render, broken down by type. Counts are monotonic —
 * they only grow during a render — and are produced atomically via {@link RayCounter#snapshot}
 * so callers always see a self-consistent tuple.
 */
public record RayCounts(long primary, long shadow, long reflect, long refract) {
    /** Sum of all four ray-type counts. */
    public long total() { return primary + shadow + reflect + refract; }
}
