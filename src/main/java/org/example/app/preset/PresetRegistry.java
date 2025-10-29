package org.example.app.preset;

import java.util.LinkedHashMap;
import java.util.Map;

/** Ordered registry of presets (keeps tab order stable). */
public final class PresetRegistry {
    private PresetRegistry() {}

    public static Map<String, Preset> all() {
        Map<String, Preset> m = new LinkedHashMap<>();
        // Tab order:
        add(m, new GeneralPreset());
        add(m, new CreativePreset());
        add(m, new ProfessionalPreset());
        add(m, new AcademicPreset());
        add(m, new CodeDocPreset());
        return m;
    }

    private static void add(Map<String, Preset> map, Preset p) {
        map.put(p.key(), p);
    }
}