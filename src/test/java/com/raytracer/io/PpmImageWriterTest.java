package com.raytracer.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PpmImageWriterTest {

    private static int argb(int r, int g, int b) {
        return 0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    @Test
    void formatIdentifierIsPpm() {
        assertEquals("ppm", new PpmImageWriter().format());
    }

    /**
     * Round-trip: write a 2x2 image (with distinct colours per pixel) and decode the
     * resulting P6 file. PPM is top-down on disk; the renderer buffer is bottom-up,
     * so the writer must flip — the decoded byte for pixel (0, 0) (top-left of the
     * file) should equal the renderer pixel at the bottom-left of the buffer.
     */
    @Test
    void writesBytesIdenticalToReferenceP6Layout(@TempDir Path tmp) throws IOException {
        // Renderer buffer (bottom-up):
        //   row 0 (bottom): RED   GREEN
        //   row 1 (top):    BLUE  WHITE
        int[] pixels = {
                argb(255, 0, 0),     argb(0, 255, 0),
                argb(0, 0, 255),     argb(255, 255, 255)
        };

        Path out = tmp.resolve("test.ppm");
        Path returned = new PpmImageWriter().write(out, pixels, 2, 2);
        assertEquals(out, returned);

        byte[] bytes = Files.readAllBytes(out);
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            assertEquals('P', in.read());
            assertEquals('6', in.read());
            assertEquals('\n', in.read());

            byte[] dimsAndMax = "2 2\n255\n".getBytes(StandardCharsets.US_ASCII);
            for (byte b : dimsAndMax) assertEquals(b & 0xFF, in.read(), "header byte");

            // Top row of file = bottom row of buffer flipped = renderer row 1: BLUE, WHITE.
            // (Top row of disk file is buffer row (height-1) = row 1.)
            assertRgb(in,   0,   0, 255);
            assertRgb(in, 255, 255, 255);
            assertRgb(in, 255,   0,   0);
            assertRgb(in,   0, 255,   0);

            assertEquals(-1, in.read(), "no trailing bytes");
        }
    }

    private static void assertRgb(InputStream in, int r, int g, int b) throws IOException {
        assertEquals(r, in.read() & 0xFF);
        assertEquals(g, in.read() & 0xFF);
        assertEquals(b, in.read() & 0xFF);
    }
}
