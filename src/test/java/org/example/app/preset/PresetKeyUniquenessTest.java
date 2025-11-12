package org.example.app.preset;

import org.junit.jupiter.api.Test;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PresetKeyUniquenessTest {

    @Test
    void keys_areStableKnownSet() {
        Map<String, Preset> map = PresetRegistry.all();
        Set<String> expected = Set.of("general", "creative", "professional", "academic", "codedoc");
        assertEquals(expected, map.keySet(), "Preset keys changed; update tests and tab order if intentional");
    }
}
