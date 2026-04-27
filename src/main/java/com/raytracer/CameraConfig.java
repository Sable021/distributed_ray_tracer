package com.raytracer;

/**
 * Immutable camera and screen-plane parameters. Wraps the constants that were previously
 * scattered as {@code static final} fields in {@link Renderer}, making them injectable from
 * a JSON scene file or overridable in tests without touching the renderer.
 */
public record CameraConfig(
        double[] eye,
        double scrWxl, double scrWxr,
        double scrHyb, double scrHyt, double scrZ,
        double dofLensWidth, double dofLensHeight, double dofFocalDist
) {
    /** Returns the camera configuration that matches the original hardcoded C++ scene. */
    public static CameraConfig defaults() {
        return new CameraConfig(
                new double[]{-0.3, 3.0, 11.0},
                -3.0, 3.0, 1.25, 5.75, 8.2,
                0.4, 0.4, 3.6
        );
    }
}
