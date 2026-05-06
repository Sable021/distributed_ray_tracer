package com.raytracer.scene;

import com.raytracer.CameraConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SceneFormatsTest {

    /** Null path → built-in (hardcoded) scene. */
    @Test
    void nullPathLoadsBuiltInScene() throws IOException {
        SceneFormat.LoadedScene loaded = SceneFormats.load(null);
        assertNotNull(loaded.scene());
        // Default camera is hand-edited via CameraConfig.defaults — confirm we got that
        // exact eye, not a JSON-overridden one.
        assertArrayEquals(CameraConfig.defaults().eye(), loaded.camera().eye(), 0.0);
    }

    /** Non-null path with a {@code .json} extension routes to {@link JsonSceneFormat}. */
    @Test
    void jsonPathRoutesToJsonFormat() throws IOException {
        SceneFormat.LoadedScene loaded = SceneFormats.load(Path.of("classic.scene.json"));
        assertNotNull(loaded.scene());
        // classic.scene.json sets a non-default eye; if dispatch went to HardcodedSceneFormat
        // by mistake, this assertion would still pass — so verify a JSON-only field instead.
        assertEquals(17, loaded.scene().numActive);
    }

    @Test
    void unrecognisedExtensionIsRejected() {
        // No registered format accepts ".txt"; SceneFormats must surface that as an
        // IllegalArgumentException rather than silently picking the hardcoded fallback.
        assertThrows(IllegalArgumentException.class,
                     () -> SceneFormats.load(Path.of("scene.txt")));
    }
}
