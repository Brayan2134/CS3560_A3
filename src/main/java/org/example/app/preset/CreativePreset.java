package org.example.app.preset;

import org.example.app.model.WritingConfig;

public class CreativePreset implements Preset {
    @Override public String key() { return "creative"; }
    @Override public String title() { return "Creative"; }

    @Override
    public WritingConfig defaults() {
        return new WritingConfig(
                "gpt-4o-mini", 0.8, 900,
                WritingConfig.Tone.INFORMAL, "none",
                WritingConfig.TextMode.EXPAND
        );
    }

    @Override
    public String defaultInstruction() {
        return """
               You are a creative writing partner. Use vivid language, imagery, and varied rhythm.
               Offer fresh angles and avoid cliches. Respect the user's constraints and topic.
               """;
    }

    @Override
    public PresetCapabilities capabilities() {
        // Hide Style (per your request), keep TextMode + Translate
        return PresetCapabilities.hideStyle();
    }
}