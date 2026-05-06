package com.raytracer.scene;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JsonSceneFormatTest {

    @Test
    void acceptsJsonExtensionRegardlessOfCase() {
        JsonSceneFormat fmt = new JsonSceneFormat();
        assertTrue(fmt.accepts(Path.of("scene.json")));
        assertTrue(fmt.accepts(Path.of("SCENE.JSON")));
        assertTrue(fmt.accepts(Path.of("path", "with", "dirs", "x.json")));
    }

    @Test
    void rejectsNullAndNonJsonExtensions() {
        JsonSceneFormat fmt = new JsonSceneFormat();
        assertFalse(fmt.accepts(null));
        assertFalse(fmt.accepts(Path.of("scene.txt")));
        assertFalse(fmt.accepts(Path.of("scene")));
    }

    @Test
    void loadsClassicSceneFixtureFromRepoRoot() throws IOException {
        Path classic = Path.of("classic.scene.json");
        // Test runs from the project root; sanity-check the fixture is present.
        assertTrue(Files.exists(classic), "classic.scene.json must be in the repo root");

        SceneFormat.LoadedScene loaded = new JsonSceneFormat().load(classic);
        assertNotNull(loaded.scene());
        assertNotNull(loaded.camera());
        // classic.scene.json mirrors the built-in scene: 17 objects (5 planes + 4 spheres
        // + 4 triangles + 2 quads as area-light surfaces + 2 padding) and 2 area lights.
        assertEquals(17, loaded.scene().numActive);
        assertEquals(2, loaded.scene().lights.size());
        // Camera eye matches the JSON literal.
        assertArrayEquals(new double[]{-0.3, 3.0, 11.0}, loaded.camera().eye(), 0.0);
    }

    @Test
    void rejectsMalformedJsonMissingObjectsArray(@TempDir Path tmp) throws IOException {
        Path bogus = tmp.resolve("bad.json");
        Files.writeString(bogus, "{ \"camera\": {} }");
        JsonSceneFormat fmt = new JsonSceneFormat();
        assertThrows(IllegalArgumentException.class, () -> fmt.load(bogus));
    }
}
