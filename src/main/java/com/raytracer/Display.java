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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JavaFX entry. Hosts a 1440x1080 window backed by a {@link WritableImage}, runs the
 * {@link Renderer} on a background platform thread, and uploads each completed scanline
 * into the image via {@link Platform#runLater(Runnable)} so the picture fills in
 * progressively as it renders.
 *
 * <p>On completion, writes the same image file as the headless path (via
 * {@link ImageOut#write}) and updates the window title with the elapsed time. Render
 * thread is a daemon so closing the window kills the JVM cleanly even mid-render.
 */
public class Display extends Application {

    /**
     * JavaFX lifecycle entry. Builds the window, kicks off the render thread, and
     * returns immediately — rendering proceeds asynchronously.
     */
    @Override
    public void start(Stage stage) {
        Args args = Args.parse(getParameters().getRaw().toArray(new String[0]));

        final int w = args.width;
        final int h = args.height;

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
        final AtomicInteger rowsCompleted = new AtomicInteger(0);

        Thread render = Thread.ofPlatform().name("renderer").daemon(true).unstarted(() -> {
            Scene scene = Scene.initialise();
            Renderer renderer = new Renderer(scene, args.mode, args.gridX, args.gridY,
                                             args.maxDepth, w, h);

            renderer.setRowListener((row, pixels, width) -> {
                // Renderer is bottom-up (row 0 = bottom of image); FX image is top-down.
                int dstRow = h - 1 - row;
                int[] rowCopy = new int[width];
                System.arraycopy(pixels, row * width, rowCopy, 0, width);
                int rowsDone = rowsCompleted.incrementAndGet();
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
                Path out = ImageOut.write(args.format, pixels, renderer.getHeight(), renderer.getWidth());
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
