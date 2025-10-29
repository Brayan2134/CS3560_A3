package org.example.app.preset;

import org.example.app.model.WritingConfig;

public class GeneralPreset implements Preset {
    @Override public String key() { return "general"; }
    @Override public String title() { return "General"; }

    @Override
    public WritingConfig defaults() {
        return new WritingConfig(
                "gpt-4o-mini", 0.5, 800,
                WritingConfig.Tone.NEUTRAL, "none",
                WritingConfig.TextMode.SAME
        );
    }

    @Override
    public String defaultInstruction() {
        return """
               You are a helpful assistant. Improve clarity while preserving meaning.
               Apply tone/grammar/style as specified by the UI knobs.
               """;
    }

    @Override
    public PresetCapabilities capabilities() {
        return PresetCapabilities.allVisible();
    }
}