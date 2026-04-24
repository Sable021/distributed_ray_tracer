package com.raytracer;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.nio.file.Path;

/** Throwaway stats tool for phase-3 smoke-testing. Reads a P6 PPM and prints stats. */
public final class PpmStats {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: PpmStats <file.ppm>");
            return;
        }
        Path p = Path.of(args[0]);
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(p.toFile()))) {
            // Read ASCII header (3 whitespace-separated tokens after "P6")
            String magic = readToken(in);
            if (!"P6".equals(magic)) { System.err.println("Not P6: " + magic); return; }
            int w = Integer.parseInt(readToken(in));
            int h = Integer.parseInt(readToken(in));
            int maxv = Integer.parseInt(readToken(in));
            in.read(); // consume the single whitespace before binary data

            long sumR = 0, sumG = 0, sumB = 0;
            long darkPx = 0, brightPx = 0;
            int minR = 255, maxR = 0, minG = 255, maxG = 0, minB = 255, maxB = 0;
            long total = (long) w * h;

            for (long i = 0; i < total; i++) {
                int r = in.read(), g = in.read(), b = in.read();
                sumR += r; sumG += g; sumB += b;
                if (r < minR) minR = r; if (r > maxR) maxR = r;
                if (g < minG) minG = g; if (g > maxG) maxG = g;
                if (b < minB) minB = b; if (b > maxB) maxB = b;
                if (r + g + b < 10) darkPx++;
                if (r > 240 && g > 240 && b > 240) brightPx++;
            }

            System.out.printf("File: %s  (%d x %d, maxval=%d)%n", p, w, h, maxv);
            System.out.printf("R: min=%d  max=%d  mean=%.1f%n", minR, maxR, sumR / (double) total);
            System.out.printf("G: min=%d  max=%d  mean=%.1f%n", minG, maxG, sumG / (double) total);
            System.out.printf("B: min=%d  max=%d  mean=%.1f%n", minB, maxB, sumB / (double) total);
            System.out.printf("Near-black pixels: %d (%.2f%%)%n", darkPx, 100.0 * darkPx / total);
            System.out.printf("Near-white pixels: %d (%.2f%%)%n", brightPx, 100.0 * brightPx / total);
        }
    }

    private static String readToken(BufferedInputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        int c;
        // skip leading whitespace and comment lines
        while ((c = in.read()) != -1) {
            if (c == '#') {
                while ((c = in.read()) != -1 && c != '\n') { /* skip */ }
                continue;
            }
            if (!Character.isWhitespace(c)) break;
        }
        while (c != -1 && !Character.isWhitespace(c)) {
            sb.append((char) c);
            c = in.read();
        }
        return sb.toString();
    }
}
