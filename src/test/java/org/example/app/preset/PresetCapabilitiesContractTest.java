package org.example.app.preset;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PresetCapabilitiesContractTest {

    @Test
    void creative_hidesStyle_only() {
        Map<String, Preset> map = PresetRegistry.all();
        Preset p = map.get("creative");
        assertNotNull(p, "creative preset must exist");

        PresetCapabilities c = p.capabilities();
        assertNotNull(c);

        assertFalse(c.showStyle(), "Creative should hide Style");
        assertTrue(c.showTextMode(), "Creative should show Text Mode");
        assertTrue(c.showTranslation(), "Creative should show Translation");
    }

    @Test
    void codedoc_hidesStyle_textMode_translation() {
        Map<String, Preset> map = PresetRegistry.all();
        Preset p = map.get("codedoc");
        assertNotNull(p, "codedoc preset must exist");

        PresetCapabilities c = p.capabilities();
        assertNotNull(c);

        assertFalse(c.showStyle(), "CodeDoc should hide Style");
        assertFalse(c.showTextMode(), "CodeDoc should hide Text Mode");
        assertFalse(c.showTranslation(), "CodeDoc should hide Translation");
    }

    @Test
    void general_professional_academic_showAll() {
        Map<String, Preset> map = PresetRegistry.all();

        for (String k : new String[]{"general", "professional", "academic"}) {
            Preset p = map.get(k);
            assertNotNull(p, k + " preset must exist");
            PresetCapabilities c = p.capabilities();
            assertNotNull(c);

            assertTrue(c.showStyle(), k + " should show Style");
            assertTrue(c.showTextMode(), k + " should show Text Mode");
            assertTrue(c.showTranslation(), k + " should show Translation");
        }
    }
}