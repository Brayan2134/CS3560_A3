package org.example.app.preset;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PresetDefaultsNonNullTest {

    @Test
    void everyPreset_hasKeyTitleDefaultsAndInstruction() {
        Map<String, Preset> map = PresetRegistry.all();

        map.forEach((key, preset) -> {
            assertNotNull(preset, "Preset instance null for key " + key);

            // key() must match map key
            assertEquals(key, preset.key(), "Preset.key() should equal the registry key");

            // titles for tabs should be non-blank
            String title = preset.title();
            assertNotNull(title, key + ": title() must not be null");
            assertFalse(title.isBlank(), key + ": title() must not be blank");

            // defaults present (we do not assert individual values to avoid coupling)
            assertNotNull(preset.defaults(), key + ": defaults() must not be null");

            // instruction present and usable in UI
            String instr = preset.defaultInstruction();
            assertNotNull(instr, key + ": defaultInstruction() must not be null");
            assertFalse(instr.isBlank(), key + ": defaultInstruction() must not be blank");

            // capabilities object present
            assertNotNull(preset.capabilities(), key + ": capabilities() must not be null");
        });
    }
}