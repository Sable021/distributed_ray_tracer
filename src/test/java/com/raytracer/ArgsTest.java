package com.raytracer;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ArgsTest {

    @Test
    void defaultsWhenNoFlags() {
        Args a = Args.parse(new String[]{});
        assertFalse(a.headless);
        assertFalse(a.printUsage);
        assertEquals(Renderer.Mode.SUPERSAMPLED, a.mode);
        assertEquals(8, a.gridX);
        assertEquals(8, a.gridY);
        assertEquals(6, a.maxDepth);
        assertEquals(Renderer.DEFAULT_WIDTH, a.width);
        assertEquals(Renderer.DEFAULT_HEIGHT, a.height);
        assertEquals("ppm", a.format);
        assertNull(a.scenePath);
        assertNull(a.outPath);
        assertEquals(4, a.shadowSamples);
        assertFalse(a.tonemap);
    }

    @Test
    void headlessFlag() {
        assertTrue(Args.parse(new String[]{"--headless"}).headless);
    }

    @Test
    void tonemapFlag() {
        assertTrue(Args.parse(new String[]{"--tonemap"}).tonemap);
    }

    @Test
    void helpAndShortHelpRequestUsage() {
        assertTrue(Args.parse(new String[]{"--help"}).printUsage);
        assertTrue(Args.parse(new String[]{"-h"}).printUsage);
    }

    @Test
    void quickIsGridOneDepthTwo() {
        Args a = Args.parse(new String[]{"--quick"});
        assertEquals(1, a.gridX);
        assertEquals(1, a.gridY);
        assertEquals(2, a.maxDepth);
    }

    @Test
    void modeSupersampledAndDof() {
        assertEquals(Renderer.Mode.SUPERSAMPLED, Args.parse(new String[]{"--mode=supersampled"}).mode);
        assertEquals(Renderer.Mode.DEPTH_OF_FIELD, Args.parse(new String[]{"--mode=dof"}).mode);
    }

    @Test
    void gridSetsBothAxes() {
        Args a = Args.parse(new String[]{"--grid=4"});
        assertEquals(4, a.gridX);
        assertEquals(4, a.gridY);
    }

    @Test
    void depthOverride() {
        assertEquals(12, Args.parse(new String[]{"--depth=12"}).maxDepth);
    }

    @Test
    void widthAndHeightPositive() {
        Args a = Args.parse(new String[]{"--width=400", "--height=300"});
        assertEquals(400, a.width);
        assertEquals(300, a.height);
        assertFalse(a.printUsage);
    }

    @Test
    void widthNonPositiveRequestsUsageAndKeepsDefault() {
        Args a = Args.parse(new String[]{"--width=0"});
        assertTrue(a.printUsage);
        assertEquals(Renderer.DEFAULT_WIDTH, a.width);
    }

    @Test
    void heightNonPositiveRequestsUsageAndKeepsDefault() {
        Args a = Args.parse(new String[]{"--height=-5"});
        assertTrue(a.printUsage);
        assertEquals(Renderer.DEFAULT_HEIGHT, a.height);
    }

    @Test
    void formatLowercasedAndAccepted() {
        assertEquals("png", Args.parse(new String[]{"--format=PNG"}).format);
        assertEquals("bmp", Args.parse(new String[]{"--format=bmp"}).format);
        assertFalse(Args.parse(new String[]{"--format=ppm"}).printUsage);
    }

    @Test
    void unknownFormatRequestsUsage() {
        Args a = Args.parse(new String[]{"--format=gif"});
        assertTrue(a.printUsage);
        assertEquals("gif", a.format);
    }

    @Test
    void scenePathParsed() {
        assertEquals(Path.of("my.scene.json"),
                     Args.parse(new String[]{"--scene=my.scene.json"}).scenePath);
    }

    @Test
    void outPathParsed() {
        assertEquals(Path.of("out/render.png"),
                     Args.parse(new String[]{"--out=out/render.png"}).outPath);
    }

    @Test
    void shadowSamplesPositive() {
        assertEquals(16, Args.parse(new String[]{"--shadow-samples=16"}).shadowSamples);
    }

    @Test
    void shadowSamplesNonPositiveRequestsUsageAndKeepsDefault() {
        Args a = Args.parse(new String[]{"--shadow-samples=0"});
        assertTrue(a.printUsage);
        assertEquals(4, a.shadowSamples);
    }

    @Test
    void unknownArgRequestsUsage() {
        assertTrue(Args.parse(new String[]{"--nonsense"}).printUsage);
    }

    @Test
    void resolvedOutPathDerivesFromFormatWhenNoOut() {
        assertEquals(Path.of("raytracing.png"),
                     Args.parse(new String[]{"--format=png"}).resolvedOutPath());
    }

    @Test
    void resolvedOutPathPrefersExplicitOut() {
        assertEquals(Path.of("custom.bmp"),
                     Args.parse(new String[]{"--format=png", "--out=custom.bmp"}).resolvedOutPath());
    }

    @Test
    void multipleFlagsCombine() {
        Args a = Args.parse(new String[]{"--headless", "--quick", "--mode=dof", "--format=png"});
        assertTrue(a.headless);
        assertEquals(1, a.gridX);
        assertEquals(2, a.maxDepth);
        assertEquals(Renderer.Mode.DEPTH_OF_FIELD, a.mode);
        assertEquals("png", a.format);
        assertFalse(a.printUsage);
    }
}
