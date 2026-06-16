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

    @Test
    void rayParallelToPlaneMisses() {
        BoundedQuad q = new BoundedQuad(
                new double[]{0, -1, 0}, 9.99,
                new double[]{1, 9.99, 0.5},
                new double[]{3, 9.99, 2.5});
        Ray r = Ray.make(new double[]{2, 0, 1.5}, new double[]{1, 0, 0});  // travels in x, normal is y

        assertEquals(-1.0, q.intersect(r));
    }

    @Test
    void quadBehindRayMisses() {
        BoundedQuad q = new BoundedQuad(
                new double[]{0, -1, 0}, 9.99,
                new double[]{1, 9.99, 0.5},
                new double[]{3, 9.99, 2.5});
        Ray r = Ray.make(new double[]{2, 20, 1.5}, new double[]{0, 1, 0});  // origin above, pointing further up

        assertEquals(-1.0, q.intersect(r));
    }

    @Test
    void normalAtIsUnitNormal() {
        BoundedQuad q = new BoundedQuad(
                new double[]{0, -2, 0}, 9.99,   // unnormalised; normalAt must unit it
                new double[]{1, 9.99, 0.5},
                new double[]{3, 9.99, 2.5});
        double[] n = new double[3];

        q.normalAt(new double[]{2, 9.99, 1.5}, n);

        assertEquals(0.0, n[0], 1e-12);
        assertEquals(-1.0, n[1], 1e-12);
        assertEquals(0.0, n[2], 1e-12);
    }
}
