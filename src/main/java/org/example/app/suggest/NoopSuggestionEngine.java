package org.example.app.suggest;

/**
 * NoopSuggestionEngine
 *
 * WHAT
 *  A minimal {@link SuggestionEngine} implementation that always returns an empty
 *  {@link SuggestionResult} (i.e., no issues).
 *
 * WHY
 *  Useful for wiring and testing the UI, dependency injection, and composite engines
 *  before integrating a real provider (e.g., LanguageTool, LLM-backed grammar).
 *
 * BEHAVIOR
 *  - Synchronously returns {@link SuggestionResult#empty()} for any request.
 *  - Does not throw for any input.
 *
 * INVARIANTS
 *  - Stateless and side-effect free.
 *  - Never returns null.
 *
 * NOTES
 *  - Acts as a safe default/fallback engine.
 *  - Can be used in {@code CompositeSuggestionEngine} to keep slots reserved.
 */
public final class NoopSuggestionEngine implements SuggestionEngine {

    /**
     * Returns an empty result regardless of input.
     *
     * Description:
     *  - Ignores all fields of {@code req} and immediately returns {@link SuggestionResult#empty()}.
     *
     * @param req the suggestion request (may be null; ignored)
     * @return an empty {@link SuggestionResult} with zero issues and zero elapsed time
     *
     * Preconditions:
     *  - None.
     *
     * Postconditions:
     *  - Never throws; never returns null.
     *  - Result contains an empty issue list.
     *
     * Notes:
     *  - If the {@link SuggestionEngine} interface also supports async checks and you need symmetry,
     *    provide a trivial override that completes a future with {@code SuggestionResult.empty()}.
     */
    @Override
    public SuggestionResult check(SuggestionRequest req) { return SuggestionResult.empty(); }
}