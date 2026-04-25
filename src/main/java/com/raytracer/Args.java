package com.raytracer;

/** CLI argument parser shared by Main (headless path) and Display (JavaFX path). */
final class Args {
    boolean headless = false;
    boolean printUsage = false;
    Renderer.Mode mode = Renderer.Mode.SUPERSAMPLED;
    int gridX = 8;
    int gridY = 8;
    int maxDepth = 6;

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
