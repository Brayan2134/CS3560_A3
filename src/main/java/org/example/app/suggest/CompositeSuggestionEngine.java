package org.example.app.suggest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * CompositeSuggestionEngine
 *
 * WHAT
 *  A concrete {@link SuggestionEngine} that fans out a {@link SuggestionRequest} to multiple
 *  child engines and returns a single merged {@link SuggestionResult}.
 *
 * WHY
 *  Different providers catch different classes of issues (spelling/grammar/style). This class
 *  coordinates them while keeping callers decoupled from provider details and failure modes.
 *
 * BEHAVIOR
 *  - Iterates children in the provided order for {@link #check(SuggestionRequest)}.
 *  - Swallows per-child {@link SuggestionException}s (fail-soft) and continues.
 *  - Returns a result with the concatenation of all issues (no de-dup/sort here by design).
 *  - Uses elapsed wall-clock time for the composite and a fixed providerVersion of "composite".
 *
 * INVARIANTS
 *  - The {@code children} list is never null and is immutable after construction.
 *  - {@link #check(SuggestionRequest)} never throws because a single child failed.
 *  - {@link #checkAsync(SuggestionRequest)} completes normally even if a child future fails.
 *
 * THREAD-SAFETY
 *  This class is stateless after construction and is safe to call concurrently provided each
 *  child {@code SuggestionEngine} is itself thread-safe for concurrent use.
 */
public final class CompositeSuggestionEngine implements SuggestionEngine {

    /** Ordered, immutable list of child engines to consult for each request. */
    private final List<SuggestionEngine> children;

    /**
     * Creates a composite over the given engines (order preserved).
     *
     * @param children ordered collection of engines to invoke for every request; may be empty but not null
     *
     * Preconditions:
     *  - {@code children} is non-null; elements should be non-null.
     *
     * Postconditions:
     *  - Internal list is an unmodifiable copy preserving iteration order.
     *
     * Notes:
     *  - Place cheaper/faster engines first if caller relies on order for downstream UX.
     */
    public CompositeSuggestionEngine(List<? extends SuggestionEngine> children) {
        this.children = List.copyOf(Objects.requireNonNullElse(children, List.of()));
    }

    /**
     * Convenience varargs constructor that preserves provided order.
     *
     * @param children engines to invoke; may be empty but not null
     *
     * Delegates to {@link #CompositeSuggestionEngine(List)}.
     */
    public CompositeSuggestionEngine(SuggestionEngine... children) {
        this(List.of(children));
    }


    /**
     * Runs all child engines synchronously and merges their issues.
     *
     * Description:
     *  - Iterates each child and calls {@code child.check(req)}.
     *  - If a child throws {@link SuggestionException}, the exception is swallowed and that
     *    child contributes no issues.
     *  - Concatenates all issues into a single list. (No de-dup or sorting is performed here.)
     *  - Elapsed time is measured as wall-clock for the whole composite call.
     *
     * @param req the suggestion request (must not be null; {@code req.text} may be empty)
     * @return a {@link SuggestionResult} whose {@code issues} are the concatenation of all child issues
     * @throws SuggestionException only for unrecoverable composite-level errors (not thrown for child failures)
     *
     * Preconditions:
     *  - {@code req} is non-null.
     *
     * Postconditions:
     *  - Never fails due to a single child engine exception.
     *  - {@code providerVersion} in the result is "composite".
     *  - {@code elapsedMs} reflects composite wall-clock.
     *
     * Notes:
     *  - If you need de-duplication or deterministic sorting, do it at the caller or extend this class.
     */
    @Override
    public SuggestionResult check(SuggestionRequest req) throws SuggestionException {
        long t0 = System.nanoTime();
        List<SuggestionIssue> all = new ArrayList<>();

        for (SuggestionEngine child : children) {
            try {
                SuggestionResult r = child.check(req);
                if (r != null && r.issues != null) {
                    all.addAll(r.issues);
                }
            } catch (SuggestionException ex) {
                // Swallow per-engine failure; optionally log
                // System.err.println("[suggest] child failed: " + ex.getMessage());
            }
        }

        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
        return new SuggestionResult(all, elapsed, "composite");
    }

    /**
     * Runs all child engines asynchronously and merges their issues when all complete.
     *
     * Description:
     *  - For each child, calls {@code child.checkAsync(req)}.
     *  - If a child future completes exceptionally, it is mapped to an empty result and ignored.
     *  - Gathers lists of issues from each child and concatenates them into one list.
     *  - Elapsed time is measured as composite wall-clock across all child futures.
     *
     * @param req the suggestion request (must not be null)
     * @return a {@link CompletableFuture} that completes with a merged {@link SuggestionResult}
     *
     * Preconditions:
     *  - {@code req} is non-null.
     *
     * Postconditions:
     *  - The returned future completes normally even if one or more child futures fail.
     *  - {@code providerVersion} in the result is "composite".
     *  - {@code elapsedMs} reflects composite wall-clock across the async execution.
     *
     * Notes:
     *  - This method does not enforce a timeout. If you need one, wrap this call in a timed composition
     *    (e.g., {@code orTimeout(...)} in Java 9+ or a scheduler that cancels slow children).
     *  - No de-dup/sort is performed; callers can post-process if needed.
     */
    @Override
    public CompletableFuture<SuggestionResult> checkAsync(SuggestionRequest req) {
        long t0 = System.nanoTime();

        // run all children; on failure, map to empty
        List<CompletableFuture<List<SuggestionIssue>>> futures = new ArrayList<>();
        for (SuggestionEngine child : children) {
            CompletableFuture<List<SuggestionIssue>> f =
                    child.checkAsync(req)
                            .exceptionally(ex -> SuggestionResult.empty())   // in case the future itself failed
                            .thenApply(r -> r == null ? List.of() : r.issues);
            futures.add(f);
        }

        return CompletableFuture
                .allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(v -> {
                    List<SuggestionIssue> all = new ArrayList<>();
                    for (CompletableFuture<List<SuggestionIssue>> f : futures) {
                        try {
                            all.addAll(f.join());
                        } catch (Throwable t) {
                            // swallow any join-time failure
                        }
                    }
                    long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
                    return new SuggestionResult(all, elapsed, "composite");
                });
    }
}