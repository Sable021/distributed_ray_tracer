package com.raytracer;

import com.raytracer.io.JavaFxRenderDisplay;
import com.raytracer.io.RenderDisplay;
import com.raytracer.scene.SceneFormat;
import com.raytracer.scene.SceneFormats;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * JavaFX {@link Application} entry. Parses CLI args, builds the renderer + a
 * {@link JavaFxRenderDisplay} backed by the supplied {@link Stage}, and dispatches the
 * actual render to {@link Bootstrap#runRender} on a daemon platform thread so the FX
 * thread stays responsive.
 *
 * <p>All FX-specific UI work (window, image, ray-stats overlay, title updates) lives in
 * {@link JavaFxRenderDisplay}; this class is just the FX-framework hook.
 */
public class Display extends Application {

    /**
     * JavaFX lifecycle entry. Builds the window via {@link JavaFxRenderDisplay}, kicks
     * off the render on a daemon platform thread, and returns immediately so the FX
     * thread stays free.
     */
    @Override
    public void start(Stage stage) {
        Args args = Args.parse(getParameters().getRaw().toArray(new String[0]));

        Scene scene;
        CameraConfig camera;
        try {
            SceneFormat.LoadedScene loaded = SceneFormats.load(args.scenePath);
            scene  = loaded.scene();
            camera = loaded.camera();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            Platform.runLater(() -> stage.setTitle("Ray Tracer — failed to load scene: " + e.getMessage()));
            return;
        }

        RenderConfig renderConfig = RenderConfig.defaults()
                .withShadowSamples(args.shadowSamples)
                .withAcesTonemap(args.tonemap);
        Renderer renderer = new Renderer(scene, args.mode, args.gridX, args.gridY,
                                         args.maxDepth, args.width, args.height, camera, renderConfig);

        RenderDisplay display = new JavaFxRenderDisplay(stage, args.width, args.height,
                                                        renderer::getRayCounts);

        Thread.ofPlatform().name("renderer").daemon(true).start(() ->
                Bootstrap.runRender(renderer, display, args.resolvedOutPath(), args.format));
    }
}
