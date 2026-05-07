package com.raytracer;

import com.raytracer.io.HeadlessRenderDisplay;
import com.raytracer.io.RenderDisplay;
import com.raytracer.scene.SceneFormat;
import com.raytracer.scene.SceneFormats;
import javafx.application.Application;

/**
 * Application entry point.
 *
 * <p>Routes to one of two paths depending on the {@code --headless} flag:
 * <ul>
 *   <li><b>Headless:</b> {@link #runHeadless} synchronously renders and writes the
 *       output file. No JavaFX class is touched, making this safe to run on
 *       display-less CI machines.</li>
 *   <li><b>Default:</b> hands off to {@link Application#launch(Class, String...)}, which
 *       constructs a {@link Display} and renders progressively into a JavaFX window.</li>
 * </ul>
 *
 * <p>The {@code --headless} branch returns <b>before</b> any reference to a {@code javafx.*}
 * class is evaluated, so the JVM never class-loads JavaFX on headless runs.
 */
public class Main {

    /** Standard process entry point. See {@link Args#parse} for the supported flags. */
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

    /**
     * Initialise the scene, build the renderer + a {@link HeadlessRenderDisplay}, and
     * hand off to {@link Bootstrap#runRender}. Logs the configuration line; the bootstrap
     * + display together produce the rest of the headless stdout output unchanged.
     */
    private static void runHeadless(Args args) throws Exception {
        System.out.printf("Config: %dx%d mode=%s grid=%dx%d maxDepth=%d%n",
                args.width, args.height, args.mode, args.gridX, args.gridY, args.maxDepth);

        SceneFormat.LoadedScene loaded = SceneFormats.load(args.scenePath);
        Scene        scene  = loaded.scene();
        CameraConfig camera = loaded.camera();
        RenderConfig renderConfig = RenderConfig.defaults().withShadowSamples(args.shadowSamples).withAcesTonemap(args.tonemap);
        Renderer renderer = new Renderer(scene, args.mode, args.gridX, args.gridY,
                                         args.maxDepth, args.width, args.height, camera, renderConfig);

        RenderDisplay display = new HeadlessRenderDisplay();
        Bootstrap.runRender(renderer, display, args.resolvedOutPath(), args.format);
    }

    /** Print the supported CLI flags and a one-line description of each to stdout. */
    private static void printUsage() {
        System.out.println("Usage: ./gradlew run --args=\"[--headless] [--mode=dof] [--quick] [--grid=N] [--depth=N] [--width=N] [--height=N] [--format=ppm|png|bmp]\"");
        System.out.println("  --headless         skip JavaFX, write image file only");
        System.out.println("  --mode=supersampled|dof   rendering mode (default supersampled)");
        System.out.println("  --quick            fast smoke test: grid=1, depth=2");
        System.out.println("  --grid=N           supersample grid side (default 8)");
        System.out.println("  --depth=N          max recursion depth (default 6)");
        System.out.println("  --width=N          image width in pixels (default 1440)");
        System.out.println("  --height=N         image height in pixels (default 1080)");
        System.out.println("  --format=ppm|png|bmp      output format (default ppm)");
        System.out.println("  --out=PATH                output file path (default raytracing.<format>)");
        System.out.println("  --shadow-samples=N        area-light shadow sub-samples (default 4)");
        System.out.println("  --tonemap                 apply ACES filmic tone mapping");
        System.out.println("  --scene=PATH              load scene + camera from a JSON file");
    }
}
