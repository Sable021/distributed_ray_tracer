package com.raytracer;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public final class PpmIO {

    private PpmIO() {}

    /**
     * Write a binary P6 PPM file.
     *
     * argbPixels is in renderer order: row 0 = bottom of screen, row (height-1) = top.
     * This method flips vertically on write so the PPM has row 0 at the top of the image,
     * matching the C++ outputFile() flip behaviour.
     *
     * Pixel format: packed 0xAARRGGBB (alpha byte ignored).
     */
    public static void write(Path path, int[] argbPixels, int height, int width) throws IOException {
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(path.toFile()))) {
            String header = "P6\n" + width + " " + height + "\n255\n";
            out.write(header.getBytes(StandardCharsets.US_ASCII));
            for (int row = height - 1; row >= 0; row--) {
                for (int col = 0; col < width; col++) {
                    int argb = argbPixels[row * width + col];
                    out.write((argb >> 16) & 0xFF);  // R
                    out.write((argb >>  8) & 0xFF);  // G
                    out.write( argb        & 0xFF);  // B
                }
            }
        }
    }
}
