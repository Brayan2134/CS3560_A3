package org.example.app.suggest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * SuggestionResult
 *
 * WHAT
 *  Immutable value object that wraps a list of {@link SuggestionIssue} plus lightweight
 *  diagnostics (elapsed wall time and provider version string).
 *
 * WHY
 *  Normalizes provider output so callers (controllers/UI) can reason about a single,
 *  stable shape regardless of which engine produced the issues.
 *
 * DOES
 *  - Holds a defensive copy of issues (immutable list)
 *  - Records timing/metadata for logging and UX
 *  - Provides small helpers to apply replacements to an original string
 *
 * INVARIANTS
 *  - {@link #issues} is never null and is immutable (List.copyOf)
 *  - {@link #elapsedMs} >= 0
 *  - {@link #providerVersion} is never null ("" if unknown)
 *
 * NOTES
 *  - Issue offsets are interpreted against the *original* text (UTF-16 code units)
 *  - Helper mutation methods return *new* strings; the result object remains immutable
 */
public final class SuggestionResult {
    public final List<SuggestionIssue> issues;
    public final long elapsedMs;
    public final String providerVersion;

    /**
     * Construct a normalized result wrapper.
     *
     * DESCRIPTION
     *  Defensive-copies the provided issues, clamps negative elapsed to 0, and
     *  null-coalesces {@code providerVersion} to "".
     *
     * PRECONDITIONS
     *  - None; {@code issues} may be null (treated as empty).
     *
     * POSTCONDITIONS
     *  - {@link #issues} is non-null and unmodifiable.
     *  - {@link #elapsedMs} >= 0.
     *  - {@link #providerVersion} non-null.
     *
     * PARAMETERS
     *  @param issues          list of issues (may be null)
     *  @param elapsedMs       elapsed wall time in milliseconds (negative becomes 0)
     *  @param providerVersion provider/version tag for diagnostics (may be null → "")
     *
     * THROWS
     *  - Never throws.
     */
    public SuggestionResult(List<SuggestionIssue> issues, long elapsedMs, String providerVersion) {
        this.issues = List.copyOf(Objects.requireNonNullElse(issues, List.of()));
        this.elapsedMs = Math.max(0, elapsedMs);
        this.providerVersion = providerVersion == null ? "" : providerVersion;
    }

    /**
     * Convenience factory for an empty result with zero elapsed time and no version tag.
     *
     * RETURNS
     *  @return {@code SuggestionResult} with {@code issues=[]}, {@code elapsedMs=0}, {@code providerVersion=""}.
     *
     * NOTES
     *  - Useful for no-op engines and error fallbacks.
     */
    public static SuggestionResult empty() { return new SuggestionResult(List.of(), 0, ""); }

    /**
     * Apply the first issue's first replacement (if any) to {@code original}.
     *
     * DESCRIPTION
     *  If there is at least one issue and it has at least one suggested replacement,
     *  replaces the span {@code [start, end)} with the first replacement string. Otherwise
     *  returns {@code original} unchanged.
     *
     * PRECONDITIONS
     *  - {@code original} is non-null.
     *  - Issue offsets must be valid for {@code original}'s UTF-16 indices.
     *
     * POSTCONDITIONS
     *  - Returns a new string if a replacement was applied; otherwise returns the same
     *    reference passed in (unchanged).
     *
     * PARAMETERS
     *  @param original the original text the offsets refer to
     *
     * RETURNS
     *  @return text with the first applicable replacement applied, or {@code original}.
     *
     * NOTES
     *  - No overlap handling is needed because only one issue is considered.
     *  - If offsets are out of bounds, this will throw {@link StringIndexOutOfBoundsException}.
     */
    public String applyReplacement(String original) {
        if (issues.isEmpty()) return original;
        SuggestionIssue i = issues.get(0);
        if (i.replacements.isEmpty()) return original;
        return original.substring(0, i.start) + i.replacements.get(0) + original.substring(i.end);
    }

    /**
     * Apply the first replacement for *every* issue to {@code original}, safely.
     *
     * DESCRIPTION
     *  Sorts issues by {@code start} ASC, then applies replacements from right-to-left.
     *  This preserves index validity for earlier spans. For issues lacking replacements,
     *  the span is skipped. Overlapping issues are applied in that right-to-left order.
     *
     * PRECONDITIONS
     *  - {@code original} is non-null.
     *  - All issue offsets are valid for {@code original}'s UTF-16 indices.
     *
     * POSTCONDITIONS
     *  - Returns a new string with zero or more replacements applied; never mutates
     *    {@code original} or {@link #issues}.
     *
     * PARAMETERS
     *  @param original the original text the offsets refer to
     *
     * RETURNS
     *  @return text after applying each issue's first replacement (where available).
     *
     * NOTES
     *  - Policy: on overlap, later (rightmost) spans win because application proceeds
     *    from right to left.
     *  - Only the *first* replacement per issue is used; callers needing advanced
     *    selection should implement their own application strategy.
     *  - May throw {@link StringIndexOutOfBoundsException} if offsets are invalid.
     */
    public String applyAll(String original) {
        if (issues.isEmpty()) return original;
        var sorted = new ArrayList<>(issues);
        sorted.sort(Comparator.comparingInt(si -> si.start));
        // apply right-to-left
        String out = original;
        for (int k = sorted.size() - 1; k >= 0; k--) {
            SuggestionIssue i = sorted.get(k);
            if (i.replacements.isEmpty()) continue;
            out = out.substring(0, i.start) + i.replacements.get(0) + out.substring(i.end);
        }
        return out;
    }
}