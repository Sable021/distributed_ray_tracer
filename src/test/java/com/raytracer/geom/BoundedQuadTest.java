package com.raytracer.geom;

import com.raytracer.Ray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoundedQuadTest {

    /**
     * Mirrors Scene.initialise's ceiling area light: y = 9.99, normal pointing down,
     * a 2×2 panel at x ∈ [1,3], z ∈ [0.5, 2.5].
     */
    @Test
    void rayInsideCornersHitsTheQuad() {
        BoundedQuad q = new BoundedQuad(
                new double[]{0, -1, 0}, 9.99,
                new double[]{1, 9.99, 0.5},
                new double[]{3, 9.99, 2.5});
        Ray r = Ray.make(new double[]{2, 0, 1.5}, new double[]{0, 1, 0});

        assertEquals(9.99, q.intersect(r), 1e-9);
    }

    @Test
    void rayOutsideCornersIsRejected() {
        BoundedQuad q = new BoundedQuad(
                new double[]{0, -1, 0}, 9.99,
                new double[]{1, 9.99, 0.5},
                new double[]{3, 9.99, 2.5});
        Ray r = Ray.make(new double[]{10, 0, 1.5}, new double[]{0, 1, 0});  // x=10 is outside [1,3]

        assertEquals(-1.0, q.intersect(r));
    }

    @Test
    void rayJustBarelyInsideHits() {
        BoundedQuad q = new BoundedQuad(
                new double[]{0, -1, 0}, 9.99,
                new double[]{1, 9.99, 0.5},
                new double[]{3, 9.99, 2.5});
        Ray r = Ray.make(new double[]{1.0001, 0, 0.5001}, new double[]{0, 1, 0});

        assertEquals(9.99, q.intersect(r), 1e-9);
    }

    @Test
    void rayJustOutsideMisses() {
        BoundedQuad q = new BoundedQuad(
                new double[]{0, -1, 0}, 9.99,
                new double[]{1, 9.99, 0.5},
                new double[]{3, 9.99, 2.5});
        Ray r = Ray.make(new double[]{0.9999, 0, 1.5}, new double[]{0, 1, 0});

        assertEquals(-1.0, q.intersect(r));
    }
}
