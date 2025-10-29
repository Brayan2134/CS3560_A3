package org.example.app.preset;

import org.example.app.model.WritingConfig;

public class ProfessionalPreset implements Preset {
    @Override public String key() { return "professional"; }
    @Override public String title() { return "Professional"; }

    @Override
    public WritingConfig defaults() {
        return new WritingConfig(
                "gpt-4o-mini", 0.3, 700,
                WritingConfig.Tone.FORMAL, "none",
                WritingConfig.TextMode.SAME
        );
    }

    @Override
    public String defaultInstruction() {
        return """
               You are a professional writing assistant. Be concise, precise, and actionable.
               Use a confident but polite tone. Favor active voice and bullet points when helpful.
               """;
    }

    @Override
    public PresetCapabilities capabilities() {
        return PresetCapabilities.allVisible();
    }
}