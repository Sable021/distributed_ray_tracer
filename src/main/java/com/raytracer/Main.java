package com.raytracer;

import java.nio.file.Path;

/**
 * Entry point. Phase 3 supports headless rendering only — `--headless` is required.
 * JavaFX display comes in Phase 4.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        Args parsed = Args.parse(args);

        if (parsed.printUsage) {
            printUsage();
            return;
        }
        if (!parsed.headless) {
            System.err.println("Phase 3: --headless is required (JavaFX display lands in Phase 4).");
            printUsage();
            System.exit(1);
        }

        System.out.printf("Config: mode=%s grid=%dx%d maxDepth=%d%n",
                parsed.mode, parsed.gridX, parsed.gridY, parsed.maxDepth);

        Scene scene = Scene.initialise();
        Renderer renderer = new Renderer(scene, parsed.mode, parsed.gridX, parsed.gridY, parsed.maxDepth);

        int[] pixels = renderer.render();

        Path out = Path.of("raytracing.ppm");
        PpmIO.write(out, pixels, renderer.getHeight(), renderer.getWidth());
        System.out.println("Wrote " + out.toAbsolutePath());
    }

    private static void printUsage() {
        System.out.println("Usage: ./gradlew run --args=\"--headless [--mode=dof] [--quick] [--grid=N] [--depth=N]\"");
        System.out.println("  --headless         write raytracing.ppm only (required for Phase 3)");
        System.out.println("  --mode=supersampled|dof   rendering mode (default supersampled)");
        System.out.println("  --quick            fast smoke test: grid=1, depth=2");
        System.out.println("  --grid=N           supersample grid side (default 8)");
        System.out.println("  --depth=N          max recursion depth (default 6)");
    }

    // ------------------------------------------------------------
    // Args parsing — tiny, no library
    // ------------------------------------------------------------

    private static final class Args {
        boolean headless = false;
        boolean printUsage = false;
        Renderer.Mode mode = Renderer.Mode.SUPERSAMPLED;
        int gridX = 8;
        int gridY = 8;
        int maxDepth = 6;

        static Args parse(String[] argv) {
            Args a = new Args();
            for (String s : argv) {
                switch (s) {
                    case "--headless" -> a.headless = true;
                    case "--help", "-h" -> a.printUsage = true;
                    case "--quick" -> {
                        a.gridX = 1; a.gridY = 1; a.maxDepth = 2;
                    }
                    case "--mode=supersampled" -> a.mode = Renderer.Mode.SUPERSAMPLED;
                    case "--mode=dof"          -> a.mode = Renderer.Mode.DEPTH_OF_FIELD;
                    default -> {
                        if (s.startsWith("--grid=")) {
                            int n = Integer.parseInt(s.substring(7));
                            a.gridX = n; a.gridY = n;
                        } else if (s.startsWith("--depth=")) {
                            a.maxDepth = Integer.parseInt(s.substring(8));
                        } else {
                            System.err.println("Unknown arg: " + s);
                            a.printUsage = true;
                        }
                    }
                }
            }
            return a;
        }
    }
}
