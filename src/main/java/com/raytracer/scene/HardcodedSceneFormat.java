package com.raytracer.scene;

import com.raytracer.CameraConfig;
import com.raytracer.Scene;

import java.nio.file.Path;

/**
 * In-memory format that returns the built-in scene defined in {@link Scene#initialise()}.
 * Used when no {@code --scene=PATH} is supplied; {@link #accepts} returns {@code true}
 * for {@code null} so {@link SceneFormats} routes the no-path case here.
 */
public final class HardcodedSceneFormat implements SceneFormat {

    @Override
    public boolean accepts(Path path) {
        return path == null;
    }

    @Override
    public LoadedScene load(Path path) {
        return new LoadedScene(Scene.initialise(), CameraConfig.defaults());
    }
}
