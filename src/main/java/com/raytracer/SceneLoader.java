package com.raytracer;

import com.google.gson.*;
import com.raytracer.geom.BoundedQuad;
import com.raytracer.geom.Cylinder;
import com.raytracer.geom.Plane;
import com.raytracer.geom.Sphere;
import com.raytracer.geom.Triangle;
import com.raytracer.shading.AreaLight;
import com.raytracer.shading.CheckerTexture;
import com.raytracer.shading.Light;
import com.raytracer.shading.Material;
import com.raytracer.shading.PhongBRDF;
import com.raytracer.shading.SolidColorTexture;
import com.raytracer.shading.StripesTexture;
import com.raytracer.shading.Texture;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

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
 * {@code specular_t} is accepted for backward compatibility but currently unused.
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

        List<Light> lights = new ArrayList<>();
        for (int i = 0; i < count; i++) parseObject(arr.get(i).getAsJsonObject(), objects[i], lights);
        return new Scene(objects, count, lights);
    }

    private static void parseObject(JsonObject j, SceneObject o, List<Light> lights) {
        String type = j.get("type").getAsString();
        switch (type) {
            case "plane" -> {
                double[] normal = vec3(j.getAsJsonArray("normal"));
                double dist = dbl(j, "dist", 0.0);
                o.primitive = new Plane(normal, dist);
                o.material  = parseMaterial(j);
            }
            case "sphere" -> {
                double[] centre = vec3(j.getAsJsonArray("centre"));
                double radius = dbl(j, "radius", 1.0);
                o.primitive = new Sphere(centre, radius);
                o.material  = parseMaterial(j);
            }
            case "triangle" -> {
                JsonArray verts = j.getAsJsonArray("vertices");
                double[] v0 = vec3(verts.get(0).getAsJsonArray());
                double[] v1 = vec3(verts.get(1).getAsJsonArray());
                double[] v2 = vec3(verts.get(2).getAsJsonArray());
                double[] normal = vec3(j.getAsJsonArray("normal"));
                o.primitive = new Triangle(v0, v1, v2, normal);
                o.material  = parseMaterial(j);
            }
            case "area_light" -> {
                double[] normal = vec3(j.getAsJsonArray("normal"));
                double dist = dbl(j, "dist", 0.0);
                JsonArray cornersJson = j.getAsJsonArray("corners");
                double[][] corners = new double[4][3];
                for (int k = 0; k < 4; k++) corners[k] = vec3(cornersJson.get(k).getAsJsonArray());
                o.primitive = new BoundedQuad(normal, dist, corners[0], corners[2]);
                o.material  = null;

                double[] diffCol = j.has("diffuseColour")
                        ? vec3(j.getAsJsonArray("diffuseColour"))  : new double[]{1.0, 1.0, 1.0};
                double[] specCol = j.has("specularColour")
                        ? vec3(j.getAsJsonArray("specularColour")) : new double[]{1.0, 1.0, 1.0};
                lights.add(new AreaLight(diffCol, specCol, corners));
            }
            case "cylinder" -> {
                double[] centre = vec3(j.getAsJsonArray("centre"));
                double[] axis = vec3(j.getAsJsonArray("axis"));
                VecMath.normalize(axis);
                double radius = dbl(j, "radius", 1.0);
                double halfHeight = dbl(j, "height", 2.0) / 2.0;
                o.primitive = new Cylinder(centre, axis, radius, halfHeight);
                o.material  = parseMaterial(j);
            }
            default -> throw new IllegalArgumentException("Unknown object type: " + type);
        }

        if (j.has("skipPrimaryRays")) o.skipPrimaryRays = j.get("skipPrimaryRays").getAsBoolean();
    }

    private static Material parseMaterial(JsonObject j) {
        Texture albedo;
        if (j.has("texture")) {
            String name = j.get("texture").getAsString();
            albedo = switch (name) {
                case "checkerboard" -> new CheckerTexture();
                case "stripes"      -> new StripesTexture();
                default -> throw new IllegalArgumentException("Unknown texture: " + name);
            };
        } else if (j.has("colour")) {
            albedo = new SolidColorTexture(vec3(j.getAsJsonArray("colour")));
        } else {
            albedo = new SolidColorTexture(0.0, 0.0, 0.0);
        }

        double kd = dbl(j, "diffuse",    0.0);
        double ks = dbl(j, "specular_r", 0.0);
        int    nn = j.has("n") ? j.get("n").getAsInt() : 0;
        // specular_t accepted but unused — preserved in JSON schema for back-compat
        // double kt = dbl(j, "specular_t", 0.0);
        double refl       = dbl(j, "refl",       0.0);
        double refr       = dbl(j, "refr",       0.0);
        double rindex     = dbl(j, "rindex",     0.0);
        double glossiness = dbl(j, "glossiness", 0.0);

        return new Material(albedo, new PhongBRDF(kd, ks, nn), refl, refr, rindex, glossiness);
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
