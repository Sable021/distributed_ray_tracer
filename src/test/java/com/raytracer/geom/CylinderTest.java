package com.raytracer.geom;

import com.raytracer.Ray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CylinderTest {

    @Test
    void raySidewaysHitsCylinderSide() {
        // Vertical cylinder, radius 1, half-height 2, centred at origin
        Cylinder cyl = new Cylinder(
                new double[]{0, 0, 0},
                new double[]{0, 1, 0},
                1.0,
                2.0);
        Ray r = Ray.make(new double[]{5, 0, 0}, new double[]{-1, 0, 0});

        assertEquals(4.0, cyl.intersect(r), 1e-9);
    }

    @Test
    void rayDownwardHitsTopCap() {
        Cylinder cyl = new Cylinder(
                new double[]{0, 0, 0},
                new double[]{0, 1, 0},
                1.0,
                2.0);
        Ray r = Ray.make(new double[]{0, 5, 0}, new double[]{0, -1, 0});

        assertEquals(3.0, cyl.intersect(r), 1e-9);
    }

    @Test
    void rayPastEndCapMisses() {
        Cylinder cyl = new Cylinder(
                new double[]{0, 0, 0},
                new double[]{0, 1, 0},
                1.0,
                2.0);
        Ray r = Ray.make(new double[]{5, 10, 0}, new double[]{-1, 0, 0});  // above cylinder

        assertEquals(-1.0, cyl.intersect(r));
    }

    @Test
    void normalAtSidePointsRadiallyOutward() {
        Cylinder cyl = new Cylinder(
                new double[]{0, 0, 0},
                new double[]{0, 1, 0},
                1.0,
                2.0);
        double[] n = new double[3];
        cyl.normalAt(new double[]{1, 0, 0}, n);

        assertEquals(1.0, n[0], 1e-9);
        assertEquals(0.0, n[1], 1e-9);
        assertEquals(0.0, n[2], 1e-9);
    }
}
