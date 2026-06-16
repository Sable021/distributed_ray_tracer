package com.raytracer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntersectTest {

    @Test
    void reflectionMirrorsAboutNormal() {
        double[] out = new double[3];
        Intersect.reflection(new double[]{1, -1, 0}, new double[]{0, 1, 0}, out);

        double inv = 1.0 / Math.sqrt(2);
        assertArrayEquals(new double[]{inv, inv, 0}, out, 1e-12);  // y flips, normalized
    }

    @Test
    void refractionStraightThroughAtNormalIncidence() {
        double[] out = new double[3];
        boolean refracted = Intersect.refraction(
                new double[]{0, -1, 0}, new double[]{0, 1, 0}, 1.0, 1.5, out);

        assertTrue(refracted);
        assertArrayEquals(new double[]{0, -1, 0}, out, 1e-12);  // perpendicular ray is undeviated
    }

    @Test
    void refractionReturnsFalseOnTotalInternalReflection() {
        // Dense → rare (1.5 → 1.0) at a shallow angle exceeds the critical angle.
        double[] incident = {0.9, -0.4, 0};
        VecMath.normalize(incident);
        double[] out = new double[3];

        boolean refracted = Intersect.refraction(incident, new double[]{0, 1, 0}, 1.5, 1.0, out);

        assertFalse(refracted);  // sqCoef < 0 → TIR
    }
}
