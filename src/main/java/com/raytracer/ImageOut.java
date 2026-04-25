package com.raytracer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Dispatches image output to PpmIO (P6) or javax.imageio (PNG/BMP).
 * Renderer pixel buffer is bottom-up; PNG/BMP path flips into a top-down BufferedImage.
 */
public final class ImageOut {

    private ImageOut() {}

    /**
     * Write the renderer's pixel buffer to disk in the requested format.
     *
     * <p>For {@code "ppm"}, delegates to {@link PpmIO#write} (which itself flips
     * bottom-up to top-down inside the writer). For other formats, copies the pixels
     * row-by-row into a top-down {@link BufferedImage} and dispatches to
     * {@link ImageIO#write}, which the JRE supports out of the box for {@code "png"}
     * and {@code "bmp"}.
     *
     * @param format one of {@code "ppm"}, {@code "png"}, or {@code "bmp"}
     * @param pixels ARGB pixel buffer in bottom-up row order; alpha is ignored
     * @param height image height in pixels
     * @param width  image width in pixels
     * @return absolute path to the file just written (e.g. {@code raytracing.png})
     * @throws IOException if the underlying writer fails or rejects the format
     */
    public static Path write(String format, int[] pixels, int height, int width) throws IOException {
        if ("ppm".equals(format)) {
            Path p = Path.of("raytracing.ppm");
            PpmIO.write(p, pixels, height, width);
            return p;
        }

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            int srcRow = height - 1 - y;
            img.setRGB(0, y, width, 1, pixels, srcRow * width, width);
        }
        Path p = Path.of("raytracing." + format);
        if (!ImageIO.write(img, format, p.toFile())) {
            throw new IOException("No ImageIO writer for format: " + format);
        }
        return p;
    }
}
