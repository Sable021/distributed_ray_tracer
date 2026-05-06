package com.raytracer.scene;

import com.raytracer.CameraConfig;
import com.raytracer.Scene;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Loader for a scene description. Implementations decide whether a given path is theirs
 * ({@link #accepts}) and how to materialise it ({@link #load}).
 *
 * <p>Adding a new scene file format = one new {@code SceneFormat} implementation
 * plus one line in {@link SceneFormats}; no edits to {@link com.raytracer.Main} or
 * {@link com.raytracer.Display}.
 */
public interface SceneFormat {

    /** Whether this format can load {@code path}. May be called with {@code null}. */
    boolean accepts(Path path);

    /**
     * Load {@code path} into a {@link Scene} and accompanying {@link CameraConfig}.
     *
     * @param path source file path; for in-memory formats may be {@code null}
     */
    LoadedScene load(Path path) throws IOException;

    /** A scene paired with the camera configuration that should view it. */
    record LoadedScene(Scene scene, CameraConfig camera) {}
}
