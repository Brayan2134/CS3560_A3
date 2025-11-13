package org.example.app.suggest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * SuggestionEngine
 *
 * WHAT
 *  Strategy interface for pluggable text-suggestion providers (spelling, grammar, style).
 *  Implementations analyze a {@link SuggestionRequest} and return a {@link SuggestionResult}.
 *
 * WHY
 *  Decouples the UI/domain from concrete providers (LanguageTool, LLM, custom rules), enabling
 *  easy swapping, composition, and test doubles.
 *
 * CONTRACTS & THREADING
 *  - Implementations should be thread-safe for concurrent calls, or clearly document limitations.
 *  - Implementations SHOULD avoid blocking UI threads; heavy work belongs off the JavaFX/EDT.
 *  - Methods MUST NOT return null objects (use empty results where appropriate).
 *
 * INVARIANTS
 *  - Returned issue spans (start..end) must be valid UTF-16 offsets within request text.
 *  - Result lists may be empty but never null.
 *
 * RELATED
 *  {@link SuggestionRequest}, {@link SuggestionResult}, {@link SuggestionIssue},
 *  {@link org.example.app.suggest.CompositeSuggestionEngine}
 */

public interface SuggestionEngine {

    /**
     * Perform a synchronous analysis of the supplied request.
     *
     * Description:
     *  Executes provider logic (e.g., tokenize, run rules/ML, build issues) and returns a result.
     *  Implementations should perform NO UI blocking and may throw {@link SuggestionException}
     *  for unrecoverable provider errors (I/O, misconfiguration, etc.).
     *
     * Preconditions:
     *  - {@code req != null}; {@code req.text} may be empty but not null.
     *
     * Postconditions:
     *  - Returns a non-null {@link SuggestionResult}.
     *  - All {@link SuggestionIssue} spans are within bounds of {@code req.text}.
     *
     * @param req the analysis request (language, scope, user dictionary, etc.)
     * @return a non-null {@link SuggestionResult} (possibly with zero issues)
     * @throws SuggestionException on unrecoverable provider failure
     */
    SuggestionResult check(SuggestionRequest req) throws SuggestionException;

    /**
     * Asynchronous analysis with fail-soft default semantics.
     *
     * Description:
     *  Default implementation dispatches {@link #check(SuggestionRequest)} on a background thread.
     *  Any exception is caught and converted to {@link SuggestionResult#empty()} so callers
     *  do not have to handle failures explicitly. Providers may override to use their own
     *  schedulers, batching, or non-blocking pipelines.
     *
     * Preconditions:
     *  - None (null requests are permitted but discouraged; behavior mirrors {@code check}).
     *
     * Postconditions:
     *  - Returns a {@link CompletableFuture} that completes with a non-null {@link SuggestionResult}.
     *  - Never completes exceptionally in the default implementation.
     *
     * @param req the analysis request
     * @return future completing with a result (never exceptional by default)
     */
    default CompletableFuture<SuggestionResult> checkAsync(SuggestionRequest req) {
        return CompletableFuture.supplyAsync(() -> {
            try { return check(req); }
            catch (Throwable t) { return SuggestionResult.empty(); }
        });
    }

    /**
     * Apply a chosen replacement to the given text for a single issue.
     *
     * Description:
     *  Produces a new string where the span {@code [issue.start, issue.end)} is replaced with
     *  {@code issue.replacements.get(replacementIndex)}. Out-of-range indices or null inputs
     *  yield the original text unchanged. Engines may override to handle provider-specific
     *  transforms (e.g., multi-span edits, capitalization, whitespace rules).
     *
     * Preconditions:
     *  - {@code text} may be null (returns null).
     *  - {@code issue} non-null with a valid span for the given text; otherwise behavior is best-effort.
     *
     * Postconditions:
     *  - Returns a new string with the requested substitution, or the original text if not applicable.
     *
     * @param text original text
     * @param issue the issue whose replacement to apply
     * @param replacementIndex index into {@code issue.replacements}
     * @return updated text (or original if no change)
     */
    default String applyReplacement(String text, SuggestionIssue issue, int replacementIndex) {
        if (text == null || issue == null) return text;
        if (replacementIndex < 0 || replacementIndex >= issue.replacements.size()) return text;
        String repl = issue.replacements.get(replacementIndex);
        return text.substring(0, issue.start) + repl + text.substring(issue.end);
    }

    /**
     * Optional post-processing hook to normalize or map issues.
     *
     * Description:
     *  Allows engines to remap rule IDs, adjust severities, de-duplicate, collapse overlaps,
     *  or attach provider metadata before results reach higher layers. Default is identity.
     *
     * Preconditions:
     *  - {@code issues} may be null or empty; treat null as empty.
     *
     * Postconditions:
     *  - Returns a non-null list (possibly empty). Implementations should avoid returning null.
     *
     * @param issues list of issues to normalize
     * @return normalized list (non-null)
     */
    default List<SuggestionIssue> normalize(List<SuggestionIssue> issues) { return issues; }
}