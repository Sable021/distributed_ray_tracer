package com.raytracer;

import java.nio.file.Path;

/** Entry point — smoke test only for Phase 2. Will be expanded in Phase 3. */
public class Main {

    public static void main(String[] args) throws Exception {
        smokeTest();
    }

    private static void smokeTest() throws Exception {
        System.out.println("=== Phase 2 smoke test ===");

        // 1. PpmIO: write a tiny 4x4 gradient PPM
        int W = 4, H = 4;
        int[] pixels = new int[W * H];
        for (int row = 0; row < H; row++) {
            for (int col = 0; col < W; col++) {
                int r = col * 255 / (W - 1);
                int g = row * 255 / (H - 1);
                pixels[row * W + col] = 0xFF000000 | (r << 16) | (g << 8);
            }
        }
        Path ppm = Path.of("smoke_test.ppm");
        PpmIO.write(ppm, pixels, H, W);
        System.out.println("PpmIO.write OK -> " + ppm.toAbsolutePath());

        // 2. Textures.noise: must return a finite value
        double noiseVal = Textures.noise(new double[]{0.1, 0.2, 0.3});
        System.out.println("Textures.noise(0.1,0.2,0.3) = " + noiseVal +
                           (Double.isFinite(noiseVal) ? "  OK" : "  FAIL (NaN/Inf)"));

        // 3. Intersect.raySphereIntersect: ray along +Z hitting sphere at origin, r=1
        SceneObject sphere = new SceneObject();
        sphere.type   = SceneObject.ObjectType.SPHERE;
        sphere.radius = 1.0;
        // vectors[0] = centre = (0,0,0) — already zero-initialised

        Ray ray = new Ray();
        VecMath.set(ray.point,  0, 0, -5);   // origin 5 units in front
        VecMath.set(ray.direct, 0, 0,  1);   // pointing toward sphere

        // With C++ formula c = dot(V,V) - 2*r^2: V=(0,0,-5)->(0,0,5) from centre
        // c = 25 - 2 = 23, b = 2*dot((0,0,1),(0,0,5)) = 10
        // det = 100 - 4*23 = 8 > 0 → two hits
        double t = Intersect.raySphereIntersect(ray, sphere);
        System.out.println("Intersect.raySphereIntersect (Z-axis hit) t = " + t +
                           (t > 0 ? "  OK" : "  FAIL (expected t > 0)"));

        // 4. Sampling.getGridNumber: trace 0..63 should cover all 64 cells of 8x8 grid
        boolean[] seen = new boolean[64];
        for (int i = 0; i < 64; i++) {
            int[] xy = Sampling.getGridNumber(i, 8, 8);
            seen[xy[1] * 8 + xy[0]] = true;
        }
        int covered = 0;
        for (boolean b : seen) if (b) covered++;
        System.out.println("Sampling.getGridNumber covers " + covered +
                           "/64 cells" + (covered == 64 ? "  OK" : "  FAIL"));

        System.out.println("=== Done ===");
    }
}
