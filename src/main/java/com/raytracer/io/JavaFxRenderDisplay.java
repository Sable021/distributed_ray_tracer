package com.raytracer.io;

import com.raytracer.render.RayCounts;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * JavaFX {@link RenderDisplay} that hosts a {@link WritableImage} backed window, uploads
 * each completed scanline as it lands, and updates a live ray-count overlay using a
 * caller-provided {@link RayCounts} {@link Supplier}.
 *
 * <p>Carries the body that lived in {@code Display.start} pre-Phase-G — just rebound
 * behind {@link RenderDisplay} so {@code Display} (the JavaFX {@code Application} entry)
 * can collapse to a thin bootstrap.
 *
 * <p>Every callback that mutates UI state is marshalled to the FX thread via
 * {@link Platform#runLater}; the constructor must be invoked on the FX thread (typically
 * from {@code Application.start}).
 */
public final class JavaFxRenderDisplay implements RenderDisplay {

    private final Stage stage;
    private final WritableImage image;
    private final PixelWriter writer;
    private final Label rayStats;
    private final Supplier<RayCounts> statsSource;
    private final int height;
    private final AtomicInteger rowsCompleted = new AtomicInteger(0);
    private long startMs;

    /**
     * @param stage       the FX stage to populate (title is updated as the render runs)
     * @param width       image width in pixels
     * @param height      image height in pixels
     * @param statsSource supplier called per row to refresh the ray-count overlay
     */
    public JavaFxRenderDisplay(Stage stage, int width, int height, Supplier<RayCounts> statsSource) {
        this.stage       = stage;
        this.height      = height;
        this.statsSource = statsSource;

        this.image    = new WritableImage(width, height);
        this.writer   = image.getPixelWriter();
        this.rayStats = buildOverlay();

        ImageView view = new ImageView(image);
        StackPane root = new StackPane(view, rayStats);
        javafx.scene.Scene fxScene = new javafx.scene.Scene(root, width, height);

        stage.setTitle("Ray Tracer — rendering...");
        stage.setScene(fxScene);
        stage.setResizable(false);
        stage.show();
    }

    private static Label buildOverlay() {
        Label label = new Label("primary 0 | shadow 0 | reflect 0 | refract 0");
        label.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.55);" +
                "-fx-text-fill: white;" +
                "-fx-font-family: 'Consolas', 'Monaco', monospace;" +
                "-fx-font-size: 12px;" +
                "-fx-padding: 6 10 6 10;");
        StackPane.setAlignment(label, Pos.TOP_LEFT);
        StackPane.setMargin(label, new Insets(8));
        return label;
    }

    @Override
    public void onStart(int width, int height) {
        this.startMs = System.currentTimeMillis();
        this.rowsCompleted.set(0);
    }

    @Override
    public void onRowComplete(int row, int[] pixels, int width) {
        // Renderer is bottom-up (row 0 = bottom of image); FX image is top-down.
        int dstRow = height - 1 - row;
        int[] rowCopy = new int[width];
        System.arraycopy(pixels, row * width, rowCopy, 0, width);
        int rowsDone = rowsCompleted.incrementAndGet();
        RayCounts counts = statsSource.get();
        long elapsed = (System.currentTimeMillis() - startMs) / 1000;
        Platform.runLater(() -> {
            writer.setPixels(0, dstRow, width, 1,
                    PixelFormat.getIntArgbInstance(), rowCopy, 0, width);
            rayStats.setText(formatRayCounts(counts));
            if (rowsDone % 32 == 0 || rowsDone == height) {
                int pct = (rowsDone * 100) / height;
                stage.setTitle(String.format("Ray Tracer — %d%% (elapsed %ds)", pct, elapsed));
            }
        });
    }

    @Override
    public void onFinish(int[] pixels, Duration elapsed, Path outputPath) {
        long secs = elapsed.toSeconds();
        Platform.runLater(() ->
                stage.setTitle("Ray Tracer — done in " + secs + "s — " + outputPath.toAbsolutePath()));
    }

    @Override
    public void onError(Throwable t) {
        t.printStackTrace();
        Platform.runLater(() ->
                stage.setTitle("Ray Tracer — render failed: " + t.getMessage()));
    }

    /** Format ray counts as a single-line label, abbreviating large numbers (e.g. 1.2M, 3.4K). */
    private static String formatRayCounts(RayCounts c) {
        return String.format("primary %s | shadow %s | reflect %s | refract %s | total %s",
                abbreviate(c.primary()),
                abbreviate(c.shadow()),
                abbreviate(c.reflect()),
                abbreviate(c.refract()),
                abbreviate(c.total()));
    }

    private static String abbreviate(long n) {
        if (n < 1_000)         return Long.toString(n);
        if (n < 1_000_000)     return String.format("%.1fK", n / 1_000.0);
        if (n < 1_000_000_000) return String.format("%.1fM", n / 1_000_000.0);
        return String.format("%.2fB", n / 1_000_000_000.0);
    }
}
