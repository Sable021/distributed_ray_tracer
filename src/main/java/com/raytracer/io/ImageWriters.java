package com.raytracer.io;

/**
 * Registry / factory for {@link ImageWriter} implementations. The single dispatch
 * site for "given a format string, give me a writer".
 *
 * <p>Adding a new format is one new {@link ImageWriter} class plus one line in
 * {@link #forFormat}; no edits to {@link com.raytracer.Main} or
 * {@link com.raytracer.Display}.
 */
public final class ImageWriters {

    private ImageWriters() {}

    /**
     * @param format one of {@code "ppm"}, {@code "png"}, {@code "bmp"}
     * @throws IllegalArgumentException for unknown formats
     */
    public static ImageWriter forFormat(String format) {
        return switch (format) {
            case "ppm"        -> new PpmImageWriter();
            case "png", "bmp" -> new ImageIoImageWriter(format);
            default -> throw new IllegalArgumentException("Unknown image format: " + format);
        };
    }
}
