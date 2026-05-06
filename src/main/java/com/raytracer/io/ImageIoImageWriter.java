package com.raytracer.io;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * {@link javax.imageio.ImageIO}-backed writer for any format the JRE supports out of
 * the box (notably {@code "png"} and {@code "bmp"}). One instance per format ID;
 * pick the right one via {@link ImageWriters#forFormat(String)}.
 *
 * <p>Copies the bottom-up renderer buffer row-by-row into a top-down
 * {@link BufferedImage} of {@code TYPE_INT_RGB}, then dispatches to {@code ImageIO}.
 */
public final class ImageIoImageWriter implements ImageWriter {

    private final String format;

    public ImageIoImageWriter(String format) {
        this.format = format;
    }

    @Override
    public String format() { return format; }

    @Override
    public Path write(Path out, int[] pixels, int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            int srcRow = height - 1 - y;
            img.setRGB(0, y, width, 1, pixels, srcRow * width, width);
        }
        if (!ImageIO.write(img, format, out.toFile())) {
            throw new IOException("No ImageIO writer for format: " + format);
        }
        return out;
    }
}
