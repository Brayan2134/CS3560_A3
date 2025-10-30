package org.example.app.persistence;

import java.time.Instant;

/** Immutable snapshot of the UI state + knobs at save time. */
public final class SessionSnapshot {
    public final String presetKey;
    public final String instruction;   // preset prompt (editable per tab)
    public final String inputText;
    public final String outputText;

    public final double temperature;
    public final String toneKey;       // "informal" | "neutral" | "formal"
    public final String grammarStyle;  // "none" | "APA" | "MLA" | "Chicago"
    public final String textModeKey;   // "summarize" | "same" | "expand"
    public final String language;      // "English" | …

    public final String modelId;       // e.g., "gpt-4o-mini"
    public final int maxTokens;

    public final Instant capturedAt;

    public SessionSnapshot(
            String presetKey, String instruction, String inputText, String outputText,
            double temperature, String toneKey, String grammarStyle, String textModeKey, String language,
            String modelId, int maxTokens, Instant capturedAt) {
        this.presetKey = presetKey;
        this.instruction = instruction;
        this.inputText = inputText;
        this.outputText = outputText;
        this.temperature = temperature;
        this.toneKey = toneKey;
        this.grammarStyle = grammarStyle;
        this.textModeKey = textModeKey;
        this.language = language;
        this.modelId = modelId;
        this.maxTokens = maxTokens;
        this.capturedAt = capturedAt;
    }
}