package com.raytracer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.nio.file.Path;

/**
 * JavaFX entry. Hosts a 1024x768 window, runs the Renderer on a background platform thread,
 * and uploads completed scanlines into a WritableImage via Platform.runLater. Writes the same
 * raytracing.ppm as the headless path on completion.
 */
public class Display extends Application {

    @Override
    public void start(Stage stage) {
        Args args = Args.parse(getParameters().getRaw().toArray(new String[0]));

        final int w = Renderer.SCR_WIDTH;
        final int h = Renderer.SCR_HEIGHT;

        WritableImage image = new WritableImage(w, h);
        PixelWriter writer = image.getPixelWriter();
        ImageView view = new ImageView(image);
        StackPane root = new StackPane(view);
        javafx.scene.Scene fxScene = new javafx.scene.Scene(root, w, h);

        stage.setTitle("Ray Tracer — rendering...");
        stage.setScene(fxScene);
        stage.setResizable(false);
        stage.show();

        final long startMs = System.currentTimeMillis();

        Thread render = Thread.ofPlatform().name("renderer").daemon(true).unstarted(() -> {
            Scene scene = Scene.initialise();
            Renderer renderer = new Renderer(scene, args.mode, args.gridX, args.gridY, args.maxDepth);

            renderer.setRowListener((row, pixels, width) -> {
                // Renderer is bottom-up (row 0 = bottom of image); FX image is top-down.
                int dstRow = h - 1 - row;
                int[] rowCopy = new int[width];
                System.arraycopy(pixels, row * width, rowCopy, 0, width);
                int rowsDone = row + 1;
                Platform.runLater(() -> {
                    writer.setPixels(0, dstRow, width, 1,
                            PixelFormat.getIntArgbInstance(), rowCopy, 0, width);
                    if (rowsDone % 32 == 0 || rowsDone == h) {
                        long elapsed = (System.currentTimeMillis() - startMs) / 1000;
                        int pct = (rowsDone * 100) / h;
                        stage.setTitle(String.format("Ray Tracer — %d%% (elapsed %ds)", pct, elapsed));
                    }
                });
            });

            int[] pixels = renderer.render();

            try {
                Path out = Path.of("raytracing.ppm");
                PpmIO.write(out, pixels, renderer.getHeight(), renderer.getWidth());
                long elapsed = (System.currentTimeMillis() - startMs) / 1000;
                System.out.println("Wrote " + out.toAbsolutePath());
                Platform.runLater(() -> stage.setTitle(
                        "Ray Tracer — done in " + elapsed + "s — " + out.toAbsolutePath()));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> stage.setTitle("Ray Tracer — render failed: " + e.getMessage()));
            }
        });
        render.start();
    }
}
