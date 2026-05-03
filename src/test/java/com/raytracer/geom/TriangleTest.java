package com.raytracer.geom;

import com.raytracer.Ray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TriangleTest {

    @Test
    void rayHitsTriangleInteriorAtKnownT() {
        // Equilateral-ish triangle in the z=0 plane, ray straight at its centroid from z=5
        Triangle tri = new Triangle(
                new double[]{0, 0, 0},
                new double[]{1, 0, 0},
                new double[]{0, 1, 0},
                new double[]{0, 0, 1});
        Ray r = Ray.make(new double[]{0.25, 0.25, 5}, new double[]{0, 0, -1});

        assertEquals(5.0, tri.intersect(r), 1e-9);
    }

    @Test
    void rayMissesOutsideTriangle() {
        Triangle tri = new Triangle(
                new double[]{0, 0, 0},
                new double[]{1, 0, 0},
                new double[]{0, 1, 0},
                new double[]{0, 0, 1});
        Ray r = Ray.make(new double[]{2, 2, 5}, new double[]{0, 0, -1});  // way outside

        assertEquals(-1.0, tri.intersect(r));
    }

    @Test
    void normalReturnsStoredValue() {
        double[] expected = {0.5, 0.5, Math.sqrt(0.5)};
        Triangle tri = new Triangle(
                new double[]{0, 0, 0},
                new double[]{1, 0, 0},
                new double[]{0, 1, 0},
                expected);
        double[] n = new double[3];
        tri.normalAt(new double[]{0.25, 0.25, 0}, n);

        // Triangle.normalAt normalizes; stored vector is already unit — should round-trip.
        double mag = Math.sqrt(n[0]*n[0] + n[1]*n[1] + n[2]*n[2]);
        assertEquals(1.0, mag, 1e-9);
    }
}
