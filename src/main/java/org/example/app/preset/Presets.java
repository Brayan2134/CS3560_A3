package org.example.app.preset;

import org.example.app.model.WritingConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/** Registry of all presets (tab order = insertion order). */
public final class Presets {
    private Presets() {}

    public static final String GENERAL = "general";
    public static final String CREATIVE = "creative";
    public static final String PROFESSIONAL = "professional";
    public static final String ACADEMIC = "academic";
    public static final String CODEDOC = "codedoc";

    /** Keyed registry for easy lookup by tab id. */
    public static Map<String, Preset> all() {
        Map<String, Preset> m = new LinkedHashMap<>();
        m.put(GENERAL, preset(
                GENERAL, "General",
                new WritingConfig("gpt-4o-mini", 0.5, 800,
                        WritingConfig.Tone.NEUTRAL, "none", WritingConfig.TextMode.SAME),
                """
                You are a helpful assistant. Improve clarity while preserving meaning.
                Apply tone/grammar/style as specified by the UI knobs.
                """
        ));
        m.put(CREATIVE, preset(
                CREATIVE, "Creative",
                new WritingConfig("gpt-4o-mini", 0.8, 900,
                        WritingConfig.Tone.INFORMAL, "none", WritingConfig.TextMode.EXPAND),
                """
                You are a creative writing partner. Use vivid language, imagery, and varied sentence rhythm.
                Offer fresh angles and avoid clichés. Respect the user's constraints and topic.
                """
        ));
        m.put(PROFESSIONAL, preset(
                PROFESSIONAL, "Professional",
                new WritingConfig("gpt-4o-mini", 0.3, 700,
                        WritingConfig.Tone.FORMAL, "none", WritingConfig.TextMode.SAME),
                """
                You are a professional writing assistant. Be concise, precise, and actionable.
                Use a confident but polite tone. Favor active voice and bullet points when helpful.
                """
        ));
        m.put(ACADEMIC, preset(
                ACADEMIC, "Academic",
                new WritingConfig("gpt-4o-mini", 0.35, 850,
                        WritingConfig.Tone.FORMAL, "APA", WritingConfig.TextMode.SAME),
                """
                You are an academic editor. Maintain objective tone, define terms, and add transitions.
                Follow the selected citation style (APA/MLA/Chicago) without fabricating citations.
                """
        ));
        m.put(CODEDOC, preset(
                CODEDOC, "Code Docs",
                new WritingConfig("gpt-4o-mini", 0.4, 600,
                        WritingConfig.Tone.NEUTRAL, "none", WritingConfig.TextMode.SUMMARIZE),
                """
                You are a code documentation generator. Explain purpose, parameters, return values, examples,
                and edge cases. Prefer succinct technical language and consistent terminology.
                """
        ));
        return m;
    }

    private static Preset preset(String key, String title, WritingConfig cfg, String instruction) {
        return new Preset() {
            @Override public String key() { return key; }
            @Override public String title() { return title; }
            @Override public WritingConfig defaults() { return cfg; }
            @Override public String defaultInstruction() { return instruction; }
        };
    }
}