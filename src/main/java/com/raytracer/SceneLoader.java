package com.raytracer;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;

/**
 * Loads a scene and camera configuration from a JSON file.
 *
 * <p>Entry point is {@link #load(Path)}, which returns a {@link Loaded} record containing
 * both the populated {@link Scene} and the resolved {@link CameraConfig}. Any camera field
 * omitted from the JSON falls back to {@link CameraConfig#defaults()}.
 *
 * <h3>JSON format</h3>
 * <pre>{@code
 * {
 *   "camera": {
 *     "eye": [-0.3, 3.0, 11.0],
 *     "screenXLeft": -3.0, "screenXRight": 3.0,
 *     "screenYBottom": 1.25, "screenYTop": 5.75, "screenZ": 8.2,
 *     "dofLensWidth": 0.4, "dofLensHeight": 0.4, "dofFocalDist": 3.6
 *   },
 *   "objects": [
 *     { "type": "plane",      "normal": [...], "dist": 0.0, "colour": [...], ... },
 *     { "type": "sphere",     "centre": [...], "radius": 1.5, ... },
 *     { "type": "triangle",   "vertices": [[...],[...],[...]], "normal": [...], ... },
 *     { "type": "area_light", "normal": [...], "dist": 9.99,
 *       "corners": [[...],[...],[...],[...]], "colour": [...],
 *       "skipPrimaryRays": true }
 *   ]
 * }
 * }</pre>
 *
 * <p>Material fields ({@code diffuse}, {@code specular_r}, {@code specular_t}, {@code n},
 * {@code refl}, {@code refr}, {@code rindex}, {@code glossiness}, {@code texture},
 * {@code skipPrimaryRays}) are all optional and default to 0 / false / null when absent.
 */
public final class SceneLoader {

    private SceneLoader() {}

    /** Result of {@link #load}: a fully-populated scene paired with its camera config. */
    public record Loaded(Scene scene, CameraConfig camera) {}

    /**
     * Parse {@code file} and return the scene + camera. Throws {@link IOException} on
     * read failure and {@link IllegalArgumentException} for malformed JSON content.
     */
    public static Loaded load(Path file) throws IOException {
        JsonObject root;
        try (Reader r = Files.newBufferedReader(file)) {
            root = JsonParser.parseReader(r).getAsJsonObject();
        }
        CameraConfig camera = parseCamera(root);
        Scene        scene  = parseScene(root);
        return new Loaded(scene, camera);
    }

    // -------------------------------------------------------------------------
    // Camera
    // -------------------------------------------------------------------------

    private static CameraConfig parseCamera(JsonObject root) {
        CameraConfig def = CameraConfig.defaults();
        if (!root.has("camera")) return def;
        JsonObject c = root.getAsJsonObject("camera");

        double[] eye = c.has("eye") ? vec3(c.getAsJsonArray("eye")) : def.eye().clone();
        return new CameraConfig(
                eye,
                dbl(c, "screenXLeft",   def.scrWxl()),
                dbl(c, "screenXRight",  def.scrWxr()),
                dbl(c, "screenYBottom", def.scrHyb()),
                dbl(c, "screenYTop",    def.scrHyt()),
                dbl(c, "screenZ",       def.scrZ()),
                dbl(c, "dofLensWidth",  def.dofLensWidth()),
                dbl(c, "dofLensHeight", def.dofLensHeight()),
                dbl(c, "dofFocalDist",  def.dofFocalDist())
        );
    }

    // -------------------------------------------------------------------------
    // Scene objects
    // -------------------------------------------------------------------------

    private static Scene parseScene(JsonObject root) {
        if (!root.has("objects"))
            throw new IllegalArgumentException("scene JSON is missing required 'objects' array");

        JsonArray arr = root.getAsJsonArray("objects");
        int count = arr.size();
        SceneObject[] objects = new SceneObject[count];
        for (int i = 0; i < count; i++) objects[i] = new SceneObject();
        for (int i = 0; i < count; i++) parseObject(arr.get(i).getAsJsonObject(), objects[i]);
        return new Scene(objects, count);
    }

    private static void parseObject(JsonObject j, SceneObject o) {
        String type = j.get("type").getAsString();
        switch (type) {
            case "plane" -> {
                o.type = SceneObject.ObjectType.PLANE;
                VecMath.copy(vec3(j.getAsJsonArray("normal")), o.vectors[0]);
                o.dist = dbl(j, "dist", 0.0);
            }
            case "sphere" -> {
                o.type = SceneObject.ObjectType.SPHERE;
                VecMath.copy(vec3(j.getAsJsonArray("centre")), o.vectors[0]);
                o.radius = dbl(j, "radius", 1.0);
            }
            case "triangle" -> {
                o.type = SceneObject.ObjectType.TRIANGLE;
                JsonArray verts = j.getAsJsonArray("vertices");
                VecMath.copy(vec3(verts.get(0).getAsJsonArray()), o.vectors[0]);
                VecMath.copy(vec3(verts.get(1).getAsJsonArray()), o.vectors[1]);
                VecMath.copy(vec3(verts.get(2).getAsJsonArray()), o.vectors[2]);
                VecMath.copy(vec3(j.getAsJsonArray("normal")), o.vectors[3]);
            }
            case "area_light" -> {
                o.type    = SceneObject.ObjectType.PLANE;
                o.isLight = true;
                VecMath.copy(vec3(j.getAsJsonArray("normal")), o.vectors[0]);
                // vectors[1] = diffuse emission colour, vectors[2] = specular emission colour
                double[] diffCol = j.has("diffuseColour")
                        ? vec3(j.getAsJsonArray("diffuseColour"))  : new double[]{1.0, 1.0, 1.0};
                double[] specCol = j.has("specularColour")
                        ? vec3(j.getAsJsonArray("specularColour")) : new double[]{1.0, 1.0, 1.0};
                VecMath.copy(diffCol,  o.vectors[1]);
                VecMath.copy(specCol,  o.vectors[2]);
                JsonArray corners = j.getAsJsonArray("corners");
                for (int k = 0; k < 4; k++)
                    VecMath.copy(vec3(corners.get(k).getAsJsonArray()), o.vectors[3 + k]);
                o.dist = dbl(j, "dist", 0.0);
            }
            default -> throw new IllegalArgumentException("Unknown object type: " + type);
        }

        // Material fields — all optional
        if (j.has("colour"))      VecMath.copy(vec3(j.getAsJsonArray("colour")), o.colour);
        o.diffuse    = dbl(j, "diffuse",    0.0);
        o.specular_r = dbl(j, "specular_r", 0.0);
        o.specular_t = dbl(j, "specular_t", 0.0);
        o.n          = j.has("n") ? j.get("n").getAsInt() : 0;
        o.refl       = dbl(j, "refl",       0.0);
        o.refr       = dbl(j, "refr",       0.0);
        o.rindex     = dbl(j, "rindex",     0.0);
        o.glossiness = dbl(j, "glossiness", 0.0);
        if (j.has("texture"))         o.texture         = j.get("texture").getAsString();
        if (j.has("skipPrimaryRays")) o.skipPrimaryRays = j.get("skipPrimaryRays").getAsBoolean();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static double[] vec3(JsonArray a) {
        return new double[]{ a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble() };
    }

    private static double dbl(JsonObject j, String key, double fallback) {
        return j.has(key) ? j.get(key).getAsDouble() : fallback;
    }
}
