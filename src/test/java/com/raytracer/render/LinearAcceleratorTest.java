package com.raytracer.render;

import com.raytracer.Ray;
import com.raytracer.Scene;
import com.raytracer.SceneObject;
import com.raytracer.geom.Sphere;
import com.raytracer.shading.Light;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LinearAcceleratorTest {

    /**
     * Build a 3-sphere fixture along the -z axis. The C++ {@code 2·r²} quirk is owned by
     * {@link Sphere}; closed-form hit z-coordinates are computed below for a ray from the
     * origin pointing in -z. Ordering at depth=1 (obj[1] closest after skipping obj[2]):
     * <pre>
     *   idx 0: centre (0,0,-5), r=1   — far
     *   idx 1: centre (0,0,-3), r=1   — t = 3 - sqrt(2),    z = -(3 - sqrt(2))
     *   idx 2: centre (0,0,-2), r=0.5, skipPrimaryRays=true, t = 2 - sqrt(0.5), z = -(2 - sqrt(0.5))
     * </pre>
     */
    private static Scene fixtureScene() {
        SceneObject[] objs = new SceneObject[3];
        for (int i = 0; i < 3; i++) objs[i] = new SceneObject();

        objs[0].primitive = new Sphere(new double[]{0, 0, -5}, 1.0);
        objs[1].primitive = new Sphere(new double[]{0, 0, -3}, 1.0);
        objs[2].primitive = new Sphere(new double[]{0, 0, -2}, 0.5);
        objs[2].skipPrimaryRays = true;

        return new Scene(objs, 3, List.<Light>of());
    }

    private static Ray rayDownZ() {
        double[] origin = {0, 0, 0};
        double[] direct = {0, 0, -1};
        return Ray.make(origin, direct);
    }

    @Test
    void primaryRaysSkipObjectsFlaggedSkipPrimaryRays() {
        Accelerator acc = new LinearAccelerator();
        acc.build(fixtureScene());
        double[] hit = new double[3];

        int idx = acc.nearest(rayDownZ(), 1, hit);

        assertEquals(1, idx, "depth=1 must skip obj[2] and return obj[1]");
        double expectedZ = -(3.0 - Math.sqrt(2.0));     // C++ 2·r² quirk
        assertArrayEquals(new double[]{0, 0, expectedZ}, hit, 1e-12);
    }

    @Test
    void secondaryRaysSeeAllObjects() {
        Accelerator acc = new LinearAccelerator();
        acc.build(fixtureScene());
        double[] hit = new double[3];

        int idx = acc.nearest(rayDownZ(), 2, hit);

        assertEquals(2, idx, "depth=2 must see obj[2] and return it as nearest");
        double expectedZ = -(2.0 - Math.sqrt(0.5));     // C++ 2·r² quirk
        assertArrayEquals(new double[]{0, 0, expectedZ}, hit, 1e-12);
    }

    @Test
    void rayThatMissesAllObjectsReturnsMinusOne() {
        Accelerator acc = new LinearAccelerator();
        acc.build(fixtureScene());

        // Same origin, ray pointing in +z (away from all spheres)
        double[] origin = {0, 0, 0};
        double[] direct = {0, 0, 1};
        Ray miss = Ray.make(origin, direct);
        double[] hit = new double[3];

        assertEquals(-1, acc.nearest(miss, 1, hit));
    }

    /**
     * outIntersect must be untouched on a miss — the renderer relies on this to detect
     * misses by checking the return value rather than scanning the buffer.
     */
    @Test
    void missDoesNotMutateOutIntersect() {
        Accelerator acc = new LinearAccelerator();
        acc.build(fixtureScene());

        double[] origin = {0, 0, 0};
        double[] direct = {0, 0, 1};
        Ray miss = Ray.make(origin, direct);
        double[] hit = {7.0, 8.0, 9.0};

        acc.nearest(miss, 1, hit);
        assertArrayEquals(new double[]{7.0, 8.0, 9.0}, hit, 0.0);
    }

    @Test
    void emptySceneReturnsMinusOne() {
        Scene empty = new Scene(new SceneObject[0], 0, List.<Light>of());
        Accelerator acc = new LinearAccelerator();
        acc.build(empty);
        double[] hit = new double[3];
        assertEquals(-1, acc.nearest(rayDownZ(), 1, hit));
    }
}
