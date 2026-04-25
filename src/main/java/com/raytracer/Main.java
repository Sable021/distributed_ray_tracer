package com.raytracer;

import javafx.application.Application;

import java.nio.file.Path;

/**
 * Entry point. Routes to either the headless renderer or the JavaFX Display
 * depending on the --headless flag.
 *
 * Headless path returns before Application.launch is invoked, so no javafx.*
 * class gets loaded on display-less environments.
 */
public class Main {

    public static void main(String[] argv) throws Exception {
        Args args = Args.parse(argv);

        if (args.printUsage) {
            printUsage();
            return;
        }

        if (args.headless) {
            runHeadless(args);
            return;
        }

        Application.launch(Display.class, argv);
    }

    private static void runHeadless(Args args) throws Exception {
        System.out.printf("Config: mode=%s grid=%dx%d maxDepth=%d%n",
                args.mode, args.gridX, args.gridY, args.maxDepth);

        Scene scene = Scene.initialise();
        Renderer renderer = new Renderer(scene, args.mode, args.gridX, args.gridY, args.maxDepth);

        int[] pixels = renderer.render();

        Path out = Path.of("raytracing.ppm");
        PpmIO.write(out, pixels, renderer.getHeight(), renderer.getWidth());
        System.out.println("Wrote " + out.toAbsolutePath());
    }

    private static void printUsage() {
        System.out.println("Usage: ./gradlew run --args=\"[--headless] [--mode=dof] [--quick] [--grid=N] [--depth=N]\"");
        System.out.println("  --headless         skip JavaFX, write raytracing.ppm only");
        System.out.println("  --mode=supersampled|dof   rendering mode (default supersampled)");
        System.out.println("  --quick            fast smoke test: grid=1, depth=2");
        System.out.println("  --grid=N           supersample grid side (default 8)");
        System.out.println("  --depth=N          max recursion depth (default 6)");
    }
}
