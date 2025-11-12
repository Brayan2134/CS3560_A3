package org.example.app.persistence;

import java.time.Instant;

/**
 * SessionSnapshot.java
 *
 * What it is: Immutable payload capturing a single point-in-time writing state.
 * What it does: Bundles user inputs, model/config knobs, and produced output for persistence/export.
 * Why it exists: So revisions can store the full context needed to reproduce or export a result.
 * Notes:
 * - Designed for JSON serialization (Gson). Uses ISO-8601 for {@code capturedAt}.
 * - Immutable by convention; prefer constructing a new snapshot rather than mutating.
 */
public final class SessionSnapshot {
    /** Preset key (e.g., "General", "Academic"). Non-null, may be blank. */
    public final String presetKey;

    /** Instruction/prompt given to the model. Nullable → treated as empty on export. */
    public final String instruction;   // preset prompt (editable per tab)

    /** Raw user input text. Nullable → treated as empty on export. */
    public final String inputText;

    /** Model output text. Nullable → treated as empty on export. */
    public final String outputText;

    /** Sampling temperature (0.0–1.0 typical). */
    public final double temperature;

    /** Tone key (e.g., "formal", "neutral"). Nullable/blank allowed. */
    public final String toneKey;       // "informal" | "neutral" | "formal"

    /** Grammar/style guide (e.g., "APA", "Chicago", "none"). Nullable/blank allowed. */
    public final String grammarStyle;  // "none" | "APA" | "MLA" | "Chicago"

    /** Text mode key (e.g., "summarize", "rewrite"). Nullable/blank allowed. */
    public final String textModeKey;   // "summarize" | "same" | "expand"

    /** Target language (e.g., "English", "Spanish"). Nullable/blank allowed. */
    public final String language;      // "English" | …

    /** Model identifier (e.g., "gpt-4o-mini"). Non-null. */
    public final String modelId;       // e.g., "gpt-4o-mini"

    /** Max tokens requested for generation. */
    public final int maxTokens;

    /** Snapshot capture timestamp (UTC). Non-null. */
    public final Instant capturedAt;

    /**
     * Constructs an immutable snapshot of the current writing state.
     *
     * Does: Copies all provided values; no defensive normalization beyond null acceptance where noted.
     *
     * Preconditions:
     * - {@code presetKey}, {@code modelId}, {@code capturedAt} are non-null.
     * - {@code maxTokens} ≥ 0; {@code temperature} within your app’s accepted range (e.g., 0.0–2.0).
     *
     * Postconditions:
     * - All fields are set; instance is safe to serialize and export.
     *
     * Notes:
     * - Nullable fields (instruction/inputText/outputText/toneKey/grammarStyle/textModeKey/language)
     *   may be rendered as empty strings by exporters.
     */
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