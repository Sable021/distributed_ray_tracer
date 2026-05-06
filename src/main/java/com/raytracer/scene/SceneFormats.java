package com.raytracer.scene;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Static dispatch for scene loading. {@link #load(Path)} picks the first
 * {@link SceneFormat} whose {@link SceneFormat#accepts} returns {@code true}.
 *
 * <p>The "if {@code scenePath != null} else built-in" branch lives here, and only here.
 */
public final class SceneFormats {

    /**
     * Order matters: {@link HardcodedSceneFormat#accepts}=true only for {@code null},
     * so a non-null path falls through to {@link JsonSceneFormat}.
     */
    private static final List<SceneFormat> FORMATS = List.of(
            new HardcodedSceneFormat(),
            new JsonSceneFormat()
    );

    private SceneFormats() {}

    /**
     * Load a scene from {@code path}, or the built-in scene if {@code path} is {@code null}.
     *
     * @throws IllegalArgumentException if no registered format accepts {@code path}
     * @throws IOException              from the underlying loader
     */
    public static SceneFormat.LoadedScene load(Path path) throws IOException {
        for (SceneFormat f : FORMATS) {
            if (f.accepts(path)) return f.load(path);
        }
        throw new IllegalArgumentException("No scene format matches: " + path);
    }
}
