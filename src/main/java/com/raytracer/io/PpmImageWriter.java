package com.raytracer.io;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Writer for the binary P6 PPM format. PPM is the renderer's native output and the
 * fastest path because no encoding library is required — just a short ASCII header
 * followed by raw RGB bytes.
 *
 * <p>Flips renderer rows on write so the PPM has row 0 at the top of the image,
 * matching the C++ reference {@code outputFile()} flip behaviour.
 */
public final class PpmImageWriter implements ImageWriter {

    @Override
    public String format() { return "ppm"; }

    @Override
    public Path write(Path out, int[] pixels, int width, int height) throws IOException {
        try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(out.toFile()))) {
            String header = "P6\n" + width + " " + height + "\n255\n";
            stream.write(header.getBytes(StandardCharsets.US_ASCII));
            for (int row = height - 1; row >= 0; row--) {
                for (int col = 0; col < width; col++) {
                    int argb = pixels[row * width + col];
                    stream.write((argb >> 16) & 0xFF);  // R
                    stream.write((argb >>  8) & 0xFF);  // G
                    stream.write( argb        & 0xFF);  // B
                }
            }
        }
        return out;
    }
}
