package com.raytracer.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageWritersTest {

    @Test
    void ppmReturnsPpmImageWriter() {
        ImageWriter w = ImageWriters.forFormat("ppm");
        assertInstanceOf(PpmImageWriter.class, w);
        assertEquals("ppm", w.format());
    }

    @Test
    void pngReturnsImageIoImageWriterTaggedPng() {
        ImageWriter w = ImageWriters.forFormat("png");
        assertInstanceOf(ImageIoImageWriter.class, w);
        assertEquals("png", w.format());
    }

    @Test
    void bmpReturnsImageIoImageWriterTaggedBmp() {
        ImageWriter w = ImageWriters.forFormat("bmp");
        assertInstanceOf(ImageIoImageWriter.class, w);
        assertEquals("bmp", w.format());
    }

    @Test
    void unknownFormatThrows() {
        assertThrows(IllegalArgumentException.class, () -> ImageWriters.forFormat("jpg"));
    }

    /**
     * End-to-end smoke test for the PNG path: writes a 2x2 image and reads it back via
     * {@link ImageIO} to confirm the file is a valid decoded PNG and the renderer's
     * bottom-up buffer was flipped to a top-down PNG correctly.
     */
    @Test
    void pngWriterProducesValidPngWithFlippedRows(@TempDir Path tmp) throws IOException {
        // Renderer buffer (bottom-up):
        //   row 0 (bottom): RED   GREEN
        //   row 1 (top):    BLUE  WHITE
        int[] pixels = {
                0xFFFF0000, 0xFF00FF00,
                0xFF0000FF, 0xFFFFFFFF
        };
        Path out = tmp.resolve("test.png");
        ImageWriters.forFormat("png").write(out, pixels, 2, 2);

        BufferedImage img = ImageIO.read(out.toFile());
        assertNotNull(img, "ImageIO must decode the PNG we wrote");
        assertEquals(2, img.getWidth());
        assertEquals(2, img.getHeight());
        assertEquals(0xFF0000FF & 0xFFFFFF, img.getRGB(0, 0) & 0xFFFFFF, "top-left = bottom-up row 1, col 0 = BLUE");
        assertEquals(0xFFFF0000 & 0xFFFFFF, img.getRGB(0, 1) & 0xFFFFFF, "bottom-left = bottom-up row 0, col 0 = RED");
    }
}
