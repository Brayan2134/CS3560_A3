package org.example.app.model;

/**
 * Immutable configuration snapshot for a single generation request.
 * Keeps the knobs simple and explicit.
 */
public final class WritingConfig {

    /** Tone options the UI will expose. */
    public enum Tone { INFORMAL, NEUTRAL, FORMAL }

    /** How to treat the user text. */
    public enum TextMode { SUMMARIZE, SAME, EXPAND }

    private final String model;          // e.g., "gpt-4o-mini"
    private final double temperature;    // 0..2
    private final int maxOutputTokens;   // >0
    private final Tone tone;             // informal / neutral / formal
    private final String grammarStyle;   // "none", "APA", "MLA", "Chicago"
    private final TextMode textMode;     // summarize / same / expand

    public WritingConfig(
            String model,
            double temperature,
            int maxOutputTokens,
            Tone tone,
            String grammarStyle,
            TextMode textMode
    ) {
        if (model == null || model.isBlank()) model = "gpt-4o-mini";
        if (temperature < 0 || temperature > 2) throw new IllegalArgumentException("temperature must be in [0,2]");
        if (maxOutputTokens <= 0) throw new IllegalArgumentException("maxOutputTokens must be > 0");
        if (tone == null) tone = Tone.NEUTRAL;
        if (grammarStyle == null || grammarStyle.isBlank()) grammarStyle = "none";
        if (textMode == null) textMode = TextMode.SAME;

        this.model = model.trim();
        this.temperature = temperature;
        this.maxOutputTokens = maxOutputTokens;
        this.tone = tone;
        this.grammarStyle = grammarStyle.trim();
        this.textMode = textMode;
    }

    // Convenience "nice defaults" factory
    public static WritingConfig defaults() {
        return new WritingConfig("gpt-4o-mini", 0.5, 800, Tone.NEUTRAL, "none", TextMode.SAME);
    }

    // Getters
    public String model()            { return model; }
    public double temperature()      { return temperature; }
    public int maxOutputTokens()     { return maxOutputTokens; }
    public Tone tone()               { return tone; }
    public String grammarStyle()     { return grammarStyle; }
    public TextMode textMode()       { return textMode; }

    // Helpers for toggles your SDK expects
    public boolean expand()    { return textMode == TextMode.EXPAND; }
    public boolean summarize() { return textMode == TextMode.SUMMARIZE; }
}