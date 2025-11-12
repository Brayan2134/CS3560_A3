package org.example.app.preset;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PresetRegistryOrderTest {

    @Test
    void all_returnsFivePresets_inExpectedOrder() {
        Map<String, Preset> map = PresetRegistry.all();

        // basic shape
        assertNotNull(map, "Registry must not be null");
        assertEquals(5, map.size(), "Registry should expose 5 presets");

        // insertion order should be: general, creative, professional, academic, codedoc
        List<String> expected = List.of("general", "creative", "professional", "academic", "codedoc");
        List<String> actual = new ArrayList<>(map.keySet());
        assertEquals(expected, actual, "Tabs/presets must remain in a stable order");
    }

    @Test
    void keys_areUnique_andMapValuesNotNull() {
        Map<String, Preset> map = PresetRegistry.all();
        Set<String> seen = new HashSet<>();
        for (Map.Entry<String, Preset> e : map.entrySet()) {
            assertNotNull(e.getKey(), "Preset key must not be null");
            assertTrue(seen.add(e.getKey()), "Duplicate preset key: " + e.getKey());
            assertNotNull(e.getValue(), "Preset instance must not be null for key " + e.getKey());
        }
    }
}