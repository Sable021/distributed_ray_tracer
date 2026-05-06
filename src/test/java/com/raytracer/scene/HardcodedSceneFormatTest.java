package com.raytracer.scene;

import com.raytracer.CameraConfig;
import com.raytracer.Scene;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class HardcodedSceneFormatTest {

    @Test
    void acceptsOnlyNullPath() {
        HardcodedSceneFormat fmt = new HardcodedSceneFormat();
        assertTrue(fmt.accepts(null));
        assertFalse(fmt.accepts(Path.of("anything.json")));
    }

    @Test
    void loadReturnsBuiltInScene() {
        HardcodedSceneFormat fmt = new HardcodedSceneFormat();
        SceneFormat.LoadedScene loaded = fmt.load(null);

        assertNotNull(loaded.scene());
        assertEquals(Scene.NUM_ACTIVE, loaded.scene().numActive);
        assertEquals(2, loaded.scene().lights.size(), "built-in scene has two area lights");
        assertNotNull(loaded.camera());
        assertArrayEquals(CameraConfig.defaults().eye(), loaded.camera().eye(), 0.0);
    }
}
