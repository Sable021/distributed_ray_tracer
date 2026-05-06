package com.raytracer.io;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Writes a rendered ARGB pixel buffer to disk in a specific image format.
 *
 * <p>The buffer convention is the renderer's: row 0 is the <b>bottom</b> of the image
 * (length {@code width * height}). Implementations are responsible for flipping if the
 * destination format is top-down (as both PPM and javax.imageio expect).
 *
 * <p>Adding a new format = one new {@code ImageWriter} implementation plus one line in
 * {@link ImageWriters#forFormat(String)}; no edits to {@link com.raytracer.Main} or
 * {@link com.raytracer.Display}.
 */
public interface ImageWriter {

    /** Format identifier (e.g. {@code "ppm"}, {@code "png"}, {@code "bmp"}). */
    String format();

    /**
     * Encode {@code pixels} and write to {@code out}.
     *
     * @param out     destination file path
     * @param pixels  ARGB buffer, length {@code width * height}, bottom-up; alpha ignored
     * @param width   image width in pixels
     * @param height  image height in pixels
     * @return {@code out} (returned for chaining at call sites)
     * @throws IOException if the underlying writer fails or rejects the format
     */
    Path write(Path out, int[] pixels, int width, int height) throws IOException;
}
