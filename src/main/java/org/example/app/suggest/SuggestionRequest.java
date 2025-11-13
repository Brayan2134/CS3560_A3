package org.example.app.suggest;

import java.util.Objects;
import java.util.Set;

/**
 * SuggestionRequest
 *
 * WHAT
 *  Immutable DTO describing *what text to analyze* and *how* (language, scope, caret,
 *  category toggles, disabled rules, and a user dictionary). All engines receive this same
 *  normalized shape.
 *
 * WHY
 *  Centralizes provider-agnostic inputs so UIs/controllers don’t depend on any single
 *  engine’s request format. Keeps checking configuration explicit and easily testable.
 *
 * DOES
 *  - Holds the full text to analyze (UTF-16 offsets apply)
 *  - Conveys language (BCP-47), scope hints, and caret position for incremental checks
 *  - Carries high-level category switches and per-rule disables
 *  - Supplies a per-user dictionary of accepted words
 *
 * INVARIANTS
 *  - All fields are non-null after construction
 *  - {@code caret >= -1} (where -1 means "unknown")
 *  - Collections are immutable (empty sets by default)
 *
 * NOTES
 *  - Engines may ignore hints they don’t support, but must honor disabled rules and
 *    user dictionary where applicable.
 *  - Offsets in results must be UTF-16 code-unit indices against {@link #text}.
 */
public final class SuggestionRequest {

    /** Full body of text to analyze (UTF-16 offsets apply). */
    public final String text;

    /** BCP-47 language code like "en-US". */
    public final String language;

    /** Scope hint for providers (engines may ignore). */
    public final Range scope;

    /** Caret position hint for incremental checking (-1 if unknown). */
    public final int caret;

    /** Enabled high-level categories (e.g., "grammar","spelling","style"). */
    public final Set<String> enabledCategories;

    /** Rule IDs the user disabled. */
    public final Set<String> disabledRuleIds;

    /** User dictionary words to be treated as valid. */
    public final Set<String> userDictionary;

    /**
     * Granularity hint for engines.
     * FULL_DOC: analyze the whole document.
     * SENTENCE: analyze the current sentence (e.g., around the caret).
     * WINDOW:   analyze a small window near the caret.
     *
     * Engines may treat this as a best-effort hint.
     */
    public enum Range { FULL_DOC, SENTENCE, WINDOW }

    /**
     * Construct a fully specified immutable request.
     *
     * DESCRIPTION
     *  Initializes all fields and applies safe defaults for null/invalid inputs:
     *   - null {@code text} → ""
     *   - null {@code language} → "en-US"
     *   - null {@code scope} → {@link Range#FULL_DOC}
     *   - {@code caret < 0} → -1 (unknown)
     *   - null sets → empty, immutable sets
     *
     * PRECONDITIONS
     *  - None beyond method signature; nulls are allowed and normalized as described above.
     *
     * POSTCONDITIONS
     *  - All fields are non-null.
     *  - {@code caret >= -1}.
     *  - {@code enabledCategories}, {@code disabledRuleIds}, {@code userDictionary} are immutable.
     *
     * PARAMETERS
     *  @param text              full body of text to analyze (UTF-16 offsets apply)
     *  @param language          BCP-47 code (e.g., "en-US")
     *  @param scope             analysis scope hint (may be ignored by engines)
     *  @param caret             caret position for incremental checks, or -1 if unknown
     *  @param enabledCategories enabled high-level categories (e.g., "grammar","spelling","style")
     *  @param disabledRuleIds   provider rule IDs the user disabled
     *  @param userDictionary    user-supplied valid words to suppress false positives
     *
     * THROWS
     *  - Never throws.
     *
     * NOTES
     *  - Keep {@code userDictionary} small for performance; engines may precompile it.
     */
    public SuggestionRequest(
            String text,
            String language,
            Range scope,
            int caret,
            Set<String> enabledCategories,
            Set<String> disabledRuleIds,
            Set<String> userDictionary
    ) {
        this.text = Objects.requireNonNullElse(text, "");
        this.language = Objects.requireNonNullElse(language, "en-US");
        this.scope = scope == null ? Range.FULL_DOC : scope;
        this.caret = caret < 0 ? -1 : caret;
        this.enabledCategories = Objects.requireNonNullElse(enabledCategories, Set.of());
        this.disabledRuleIds = Objects.requireNonNullElse(disabledRuleIds, Set.of());
        this.userDictionary = Objects.requireNonNullElse(userDictionary, Set.of());
    }

    /**
     * Convenience factory for a common case: whole-document English check with defaults.
     *
     * DESCRIPTION
     *  Builds a {@code SuggestionRequest} with:
     *   - language = "en-US"
     *   - scope = FULL_DOC
     *   - caret = -1
     *   - empty enabled/disabled sets and dictionary
     *
     * PRECONDITIONS
     *  - {@code text} may be null; it will be normalized to "".
     *
     * POSTCONDITIONS
     *  - Returns a fully initialized, immutable request as specified above.
     *
     * PARAMETERS
     *  @param text full body of text to analyze
     *
     * RETURNS
     *  @return new {@code SuggestionRequest} with default configuration.
     */
    public static SuggestionRequest of(String text) {
        return new SuggestionRequest(text, "en-US", Range.FULL_DOC, -1, Set.of(), Set.of(), Set.of());
    }
}