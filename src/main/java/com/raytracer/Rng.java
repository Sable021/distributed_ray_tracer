package com.raytracer;

import java.util.SplittableRandom;

public final class Rng {

    private static final SplittableRandom RNG = new SplittableRandom(1_234_567_890L);

    private Rng() {}

    public static double uniform(double min, double max) {
        return min + RNG.nextDouble() * (max - min);
    }
}
