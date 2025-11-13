package org.example.app.suggest;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * SuggestionIssue
 *
 * WHAT
 *  Immutable value object describing a single writing issue detected by a suggestion engine
 *  (spelling, grammar, style, punctuation). Spans use UTF-16 character offsets:
 *  inclusive {@code start}, exclusive {@code end} relative to the original request text.
 *
 * WHY
 *  Normalizes heterogeneous provider outputs (LanguageTool, LLMs, custom rules) into a single,
 *  UI-friendly shape that supports de-duplication, quick-fix application, and stable selection.
 *
 * DOES
 *  - Carries provider rule identifiers and a human message
 *  - Encodes source span and severity/type for prioritization
 *  - Provides candidate replacements and optional metadata
 *  - Provides a {@link #dedupKey()} for cross-engine merging
 *
 * INVARIANTS
 *  - {@code start >= 0} and {@code end >= start}
 *  - All fields are non-null after construction; collections are defensively copied and unmodifiable
 *  - {@code id} is a stable UI key (UUID by default if not supplied)
 *
 * NOTES
 *  - Offsets are provider-agnostic; engines must convert to UTF-16 code-unit indices
 *    so Swing/Java text components can apply fixes correctly.
 *  - {@code meta} is a safe extension point for engine-specific extras (e.g., confidence scores).
 */
public final class SuggestionIssue {
    public final String id;           // stable UI id (UUID)
    public final String ruleId;       // provider rule id (e.g., LT: "MORFOLOGIK_RULE_EN_US")
    public final String provider;     // "LanguageTool", "Noop", "LLM", etc.
    public final Type type;           // SPELLING / GRAMMAR / STYLE / PUNCTUATION
    public final Severity severity;   // INFO / WARNING / ERROR
    public final int start;           // char offset
    public final int end;             // char offset
    public final String message;      // human text
    public final List<String> replacements; // possible quick fixes (0..N)
    public final Map<String, Object> meta;  // optional extra data

    /** Engine-agnostic issue category for UI filtering and styling. */
    public enum Type { SPELLING, GRAMMAR, STYLE, PUNCTUATION }

    /** Coarse severity used for ordering and theming (e.g., underline color). */
    public enum Severity { INFO, WARNING, ERROR }

    /**
     * Construct an issue with a generated UUID for {@link #id}.
     *
     * @param ruleId        provider rule identifier (e.g., "MORFOLOGIK_RULE_EN_US"); if null → "UNKNOWN_RULE"
     * @param provider      provider label (e.g., "LanguageTool", "LLM"); if null → "unknown"
     * @param type          high-level issue type (SPELLING/GRAMMAR/STYLE/PUNCTUATION) (non-null)
     * @param severity      INFO/WARNING/ERROR (null → WARNING)
     * @param start         inclusive UTF-16 character offset (must be ≥ 0)
     * @param end           exclusive UTF-16 character offset (must be ≥ start)
     * @param message       human-readable description shown in the UI (null → "")
     * @param replacements  zero or more suggested fixes; copied to an unmodifiable list (null → empty)
     * @param meta          optional provider-specific data; copied to an unmodifiable map (null → empty)
     *
     * Preconditions:
     *  - {@code start >= 0 && end >= start}
     *  - {@code type != null}
     *
     * Postconditions:
     *  - All fields non-null
     *  - {@code replacements} and {@code meta} are unmodifiable defensive copies
     *  - {@code id} is a random UUID string
     *
     * Throws:
     *  - IllegalArgumentException if span is invalid
     */
    public SuggestionIssue(
            String ruleId,
            String provider,
            Type type,
            Severity severity,
            int start,
            int end,
            String message,
            List<String> replacements,
            Map<String, Object> meta
    ) {
        this(UUID.randomUUID().toString(), ruleId, provider, type, severity, start, end, message, replacements, meta);
    }

    /**
     * Construct an issue with an explicit {@link #id}.
     *
     * @param id            stable UI identifier (null → generated UUID)
     * @param ruleId        provider rule identifier (null → "UNKNOWN_RULE")
     * @param provider      provider label (null → "unknown")
     * @param type          high-level issue type (non-null)
     * @param severity      INFO/WARNING/ERROR (null → WARNING)
     * @param start         inclusive UTF-16 character offset (≥ 0)
     * @param end           exclusive UTF-16 character offset (≥ start)
     * @param message       human-readable description (null → "")
     * @param replacements  suggested fixes; copied to an unmodifiable list (null → empty)
     * @param meta          provider-specific extras; copied to an unmodifiable map (null → empty)
     *
     * Preconditions:
     *  - {@code start >= 0 && end >= start}
     *  - {@code type != null}
     *
     * Postconditions:
     *  - All fields non-null; collections unmodifiable
     *  - {@code id} equals provided id or generated UUID if null
     *
     * Throws:
     *  - IllegalArgumentException if span is invalid
     */
    public SuggestionIssue(
            String id,
            String ruleId,
            String provider,
            Type type,
            Severity severity,
            int start,
            int end,
            String message,
            List<String> replacements,
            Map<String, Object> meta
    ) {
        if (start < 0 || end < start) throw new IllegalArgumentException("Invalid span: " + start + ".." + end);
        this.id = Objects.requireNonNullElse(id, UUID.randomUUID().toString());
        this.ruleId = Objects.requireNonNullElse(ruleId, "UNKNOWN_RULE");
        this.provider = Objects.requireNonNullElse(provider, "unknown");
        this.type = Objects.requireNonNull(type);
        this.severity = Objects.requireNonNullElse(severity, Severity.WARNING);
        this.start = start;
        this.end = end;
        this.message = Objects.requireNonNullElse(message, "");
        this.replacements = List.copyOf(Objects.requireNonNullElse(replacements, List.of()));
        this.meta = Map.copyOf(Objects.requireNonNullElse(meta, Map.of()));
    }

    /**
     * Return a stable de-duplication key computed as {@code ruleId + "@" + start + ":" + end}.
     * Provider name is intentionally ignored so the same rule/span from multiple engines collapses
     * to a single UI entry.
     *
     * @return a deterministic key suitable for merging and {@code Set}/{@code Map} usage
     *
     * Notes:
     *  - If you need provider-scoped de-duplication, prepend {@code provider + "#"} at call sites.
     */
    public String dedupKey() { return ruleId + "@" + start + ":" + end; }
}