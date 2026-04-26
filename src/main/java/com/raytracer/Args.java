package com.raytracer;

/**
 * Parsed CLI options. Shared by {@link Main} (headless path) and {@link Display}
 * (JavaFX path) so both produce identical output for the same arguments.
 *
 * <p>Each public field corresponds to one configurable knob; {@link #parse} builds
 * an {@code Args} from a raw {@code String[]} (typically {@code argv} from {@code main}
 * or {@code Application.getParameters().getRaw()}).
 */
final class Args {
    /** True when {@code --headless} was passed: skip JavaFX, write image only. */
    boolean headless = false;
    /** True when {@code --help}/{@code -h} or an unknown flag was seen — caller should show usage. */
    boolean printUsage = false;
    /** Selected render strategy ({@code --mode=supersampled|dof}). */
    Renderer.Mode mode = Renderer.Mode.SUPERSAMPLED;
    /** Supersample grid width ({@code --grid=N}, default 8). */
    int gridX = 8;
    /** Supersample grid height (set to {@code gridX} when using {@code --grid=N}). */
    int gridY = 8;
    /** Maximum recursion depth for reflected/refracted rays ({@code --depth=N}, default 6). */
    int maxDepth = 6;
    /** Image width in pixels ({@code --width=N}, default 1024). */
    int width = Renderer.DEFAULT_WIDTH;
    /** Image height in pixels ({@code --height=N}, default 768). */
    int height = Renderer.DEFAULT_HEIGHT;
    /** Output format: {@code ppm} (default), {@code png}, or {@code bmp}. */
    String format = "ppm";

    /**
     * Parse a raw {@code argv}-style array into an {@code Args}.
     *
     * <p>Unknown flags do not throw — they set {@link #printUsage} so the caller can
     * print help and exit gracefully. Flag forms supported:
     * {@code --headless}, {@code --help}, {@code -h}, {@code --quick} (alias for
     * {@code --grid=1 --depth=2}), {@code --mode=supersampled|dof}, {@code --grid=N},
     * {@code --depth=N}, {@code --format=ppm|png|bmp}.
     */
    static Args parse(String[] argv) {
        Args a = new Args();
        for (String s : argv) {
            switch (s) {
                case "--headless" -> a.headless = true;
                case "--help", "-h" -> a.printUsage = true;
                case "--quick" -> {
                    a.gridX = 1; a.gridY = 1; a.maxDepth = 2;
                }
                case "--mode=supersampled" -> a.mode = Renderer.Mode.SUPERSAMPLED;
                case "--mode=dof"          -> a.mode = Renderer.Mode.DEPTH_OF_FIELD;
                default -> {
                    if (s.startsWith("--grid=")) {
                        int n = Integer.parseInt(s.substring(7));
                        a.gridX = n; a.gridY = n;
                    } else if (s.startsWith("--depth=")) {
                        a.maxDepth = Integer.parseInt(s.substring(8));
                    } else if (s.startsWith("--width=")) {
                        int n = Integer.parseInt(s.substring(8));
                        if (n <= 0) {
                            System.err.println("--width must be positive: " + n);
                            a.printUsage = true;
                        } else {
                            a.width = n;
                        }
                    } else if (s.startsWith("--height=")) {
                        int n = Integer.parseInt(s.substring(9));
                        if (n <= 0) {
                            System.err.println("--height must be positive: " + n);
                            a.printUsage = true;
                        } else {
                            a.height = n;
                        }
                    } else if (s.startsWith("--format=")) {
                        a.format = s.substring(9).toLowerCase();
                        if (!a.format.equals("ppm") && !a.format.equals("png") && !a.format.equals("bmp")) {
                            System.err.println("Unknown format: " + a.format);
                            a.printUsage = true;
                        }
                    } else {
                        System.err.println("Unknown arg: " + s);
                        a.printUsage = true;
                    }
                }
            }
        }
        return a;
    }
}
