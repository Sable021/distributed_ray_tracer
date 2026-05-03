package com.raytracer.geom;

import com.raytracer.Ray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlaneTest {

    @Test
    void rayHitsFloorAtKnownT() {
        // y = 0 floor with normal up; ray from (0,5,0) going down
        Plane p = new Plane(new double[]{0, 1, 0}, 0.0);
        Ray r = Ray.make(new double[]{0, 5, 0}, new double[]{0, -1, 0});

        assertEquals(5.0, p.intersect(r), 1e-12);
    }

    @Test
    void rayParallelToPlaneMisses() {
        Plane p = new Plane(new double[]{0, 1, 0}, 0.0);
        Ray r = Ray.make(new double[]{0, 5, 0}, new double[]{1, 0, 0});

        assertEquals(-1.0, p.intersect(r));
    }

    @Test
    void rayBehindOriginMisses() {
        // Plane at y=0; ray at y=-5 going further down — never hits y=0 in positive t direction
        Plane p = new Plane(new double[]{0, 1, 0}, 0.0);
        Ray r = Ray.make(new double[]{0, -5, 0}, new double[]{0, -1, 0});

        assertEquals(-1.0, p.intersect(r));
    }

    @Test
    void normalIsConstantAcrossSurface() {
        Plane p = new Plane(new double[]{0, 1, 0}, 0.0);
        double[] n1 = new double[3];
        double[] n2 = new double[3];

        p.normalAt(new double[]{0, 0, 0}, n1);
        p.normalAt(new double[]{42, 0, -7}, n2);

        assertArrayEquals(n1, n2);
        assertArrayEquals(new double[]{0, 1, 0}, n1, 1e-12);
    }
}
