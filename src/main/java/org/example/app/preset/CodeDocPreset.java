package org.example.app.preset;

import org.example.app.model.WritingConfig;

public class CodeDocPreset implements Preset {
    @Override public String key() { return "codedoc"; }
    @Override public String title() { return "Code Docs"; }

    @Override
    public WritingConfig defaults() {
        return new WritingConfig(
                "gpt-4o-mini", 0.4, 600,
                WritingConfig.Tone.NEUTRAL, "none",
                WritingConfig.TextMode.SUMMARIZE // locked by controller anyway
        );
    }

    @Override
    public String defaultInstruction() {
        return """
               You are a code documentation generator. Explain purpose, parameters, return values, examples,
               and edge cases. Prefer succinct technical language and consistent terminology.
               """;
    }

    @Override
    public PresetCapabilities capabilities() {
        // Hide Style + TextMode + Translate (per your earlier requirement)
        return PresetCapabilities.codeDocs();
    }
}