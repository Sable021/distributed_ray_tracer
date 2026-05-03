package com.raytracer.geom;

import com.raytracer.Ray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SphereTest {

    /**
     * The C++ quirk preserved here is {@code c = dot(V,V) - 2*r^2} (not the textbook
     * {@code - r^2}), so the closed-form root for a ray pointing at the centre is
     * {@code t = distance ± r*sqrt(2)} rather than {@code distance ± r}. This test
     * locks the quirk in place — if it starts passing the textbook value, the C++
     * parity contract has been broken.
     */
    @Test
    void rayHitsCentredSphereAlongZ_quirkFormulaHolds() {
        Sphere s = new Sphere(new double[]{0, 0, 5}, 1.0);
        Ray r = Ray.make(new double[]{0, 0, 0}, new double[]{0, 0, 1});

        double t = s.intersect(r);

        // Quirk: t_near = 5 - sqrt(2) ≈ 3.5858 (textbook would be 4.0)
        assertEquals(5.0 - Math.sqrt(2), t, 1e-9);
    }

    @Test
    void parallelMissReturnsMinusOne() {
        Sphere s = new Sphere(new double[]{0, 0, 5}, 1.0);
        Ray r = Ray.make(new double[]{10, 0, 0}, new double[]{0, 0, 1});  // parallel, far off-axis

        assertEquals(-1.0, s.intersect(r));
    }

    @Test
    void normalAtSurfacePointIsUnitOutward() {
        Sphere s = new Sphere(new double[]{1, 2, 3}, 2.0);
        double[] surface = {3, 2, 3};   // 2 units along +x from centre
        double[] n = new double[3];

        s.normalAt(surface, n);

        assertEquals(1.0, n[0], 1e-12);
        assertEquals(0.0, n[1], 1e-12);
        assertEquals(0.0, n[2], 1e-12);
    }

    @Test
    void componentsAreDefensivelyCopied() {
        double[] centre = {1, 2, 3};
        Sphere s = new Sphere(centre, 1.0);
        centre[0] = 999;     // mutate the caller's array
        assertEquals(1.0, s.center()[0]);  // sphere unaffected
    }
}
