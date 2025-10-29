package org.example.app.preset;

import org.example.app.model.WritingConfig;

public class AcademicPreset implements Preset {
    @Override public String key() { return "academic"; }
    @Override public String title() { return "Academic"; }

    @Override
    public WritingConfig defaults() {
        return new WritingConfig(
                "gpt-4o-mini", 0.35, 850,
                WritingConfig.Tone.FORMAL, "APA",
                WritingConfig.TextMode.SAME
        );
    }

    @Override
    public String defaultInstruction() {
        return """
               You are an academic editor. Maintain objective tone, define terms, and add transitions.
               Follow the selected citation style (APA/MLA/Chicago) without fabricating citations.
               """;
    }

    @Override
    public PresetCapabilities capabilities() {
        return PresetCapabilities.allVisible();
    }
}