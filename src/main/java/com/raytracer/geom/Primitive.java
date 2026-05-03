package com.raytracer.geom;

import com.raytracer.Ray;

/**
 * A renderable geometric primitive. Each impl owns its own ray-intersection test and
 * surface-normal computation, so adding a new shape never requires editing a dispatch
 * site elsewhere.
 *
 * <p>The contract: {@link #intersect} returns the parametric distance {@code t} along
 * {@code ray} (i.e. {@code ray.point + t*ray.direct} is the hit), or {@code -1.0} on
 * miss. Sub-{@link #EPSILON} hits are rejected as self-intersections.
 *
 * <p>{@link #normalAt} writes a UNIT outward normal at {@code point} into the
 * caller-owned {@code outNormal}; no allocation in the hot path.
 *
 * <p>Sealed: all permitted impls are declared here so a switch on {@code Primitive} is
 * exhaustive when needed (e.g. a future BVH builder), but the renderer's hot path
 * dispatches polymorphically and never switches.
 */
public sealed interface Primitive permits Sphere, Plane, Triangle, Cylinder, BoundedQuad {

    /** Minimum {@code t} treated as a real hit. Smaller values are self-intersections. */
    double EPSILON = 0.0001;

    /** @return parametric {@code t} along {@code ray}, or {@code -1.0} on miss. */
    double intersect(Ray ray);

    /** Writes the unit outward normal at {@code point} into {@code outNormal} (length 3). */
    void normalAt(double[] point, double[] outNormal);
}
