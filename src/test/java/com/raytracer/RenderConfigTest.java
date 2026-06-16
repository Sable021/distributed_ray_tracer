package com.raytracer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RenderConfigTest {

    @Test
    void defaultsMatchCppConstants() {
        RenderConfig c = RenderConfig.defaults();
        assertArrayEquals(new double[]{0.05, 0.05, 0.05}, c.globalAmb(), 0.0);
        assertEquals(4, c.areaLightSubSamples());
        assertEquals(0.6, c.refractiveShadowAttenuation());
        assertEquals(4, c.glossyMaxDepth());
        assertFalse(c.acesTonemap());
    }

    @Test
    void withShadowSamplesReplacesOnlyThatField() {
        RenderConfig c = RenderConfig.defaults().withShadowSamples(16);
        assertEquals(16, c.areaLightSubSamples());
        assertArrayEquals(new double[]{0.05, 0.05, 0.05}, c.globalAmb(), 0.0);
        assertEquals(0.6, c.refractiveShadowAttenuation());
        assertEquals(4, c.glossyMaxDepth());
        assertFalse(c.acesTonemap());
    }

    @Test
    void withAcesTonemapTogglesOnlyThatField() {
        RenderConfig c = RenderConfig.defaults().withAcesTonemap(true);
        assertTrue(c.acesTonemap());
        assertEquals(4, c.areaLightSubSamples());
    }

    @Test
    void withersDoNotMutateOriginal() {
        RenderConfig base = RenderConfig.defaults();
        base.withShadowSamples(99);
        base.withAcesTonemap(true);
        assertEquals(4, base.areaLightSubSamples());
        assertFalse(base.acesTonemap());
    }
}
