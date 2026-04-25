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
