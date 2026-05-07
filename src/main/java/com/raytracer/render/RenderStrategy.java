package com.raytracer.render;

/**
 * Per-pixel rendering policy. Implementations decide how the {@code gridX * gridY} rays
 * for one pixel are constructed (eye-through-screen, lens-through-focal-point, etc.) and
 * fold the resulting samples into a single RGB triple.
 *
 * <p>The strategy owns its own camera + integrator references (passed at construction time);
 * callers only supply per-pixel state — the screen-plane cell coordinates and the
 * supersample grid size — plus a caller-owned output buffer.
 *
 * <p>Implementations must average their {@code gridX * gridY} samples internally, so
 * {@code outRgb} is the final unscaled colour ready for tone mapping and ARGB packing.
 */
public interface RenderStrategy {

    /**
     * Shade one pixel. Fires {@code gridX * gridY} rays through the cell at
     * {@code [scrX, scrX+scrDX) × [scrY, scrY+scrDY)} on the screen plane and writes the
     * averaged colour into {@code outRgb}.
     *
     * @param scrX   bottom-left x of the pixel's cell on the screen plane
     * @param scrY   bottom-left y of the pixel's cell on the screen plane
     * @param scrDX  pixel cell width on the screen plane
     * @param scrDY  pixel cell height on the screen plane
     * @param gridX  supersample grid width  (rays per pixel = gridX * gridY)
     * @param gridY  supersample grid height
     * @param outRgb caller-owned RGB[3] receiving the averaged colour
     */
    void shadePixel(double scrX, double scrY, double scrDX, double scrDY,
                    int gridX, int gridY, double[] outRgb);
}
