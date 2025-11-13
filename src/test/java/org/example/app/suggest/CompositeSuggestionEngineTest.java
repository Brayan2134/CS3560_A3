package org.example.app.suggest;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for CompositeSuggestionEngine.
 *
 * What we assert (without over-specifying internals):
 *  - Aggregates issues from multiple child engines.
 *  - Returns a valid, non-null result when some child engines throw.
 *  - With no children, returns an empty result.
 *  - checkAsync() completes and returns something sensible.
 */
class CompositeSuggestionEngineTest {

    // --- tiny fakes ----------------------------------------------------------

    /** A simple engine that flags a single token with a fixed replacement. */
    static final class TokenFlagger implements SuggestionEngine {
        private final String token;
        private final String replacement;
        private final String provider;
        private final String ruleId;

        TokenFlagger(String token, String replacement, String provider, String ruleId) {
            this.token = token;
            this.replacement = replacement;
            this.provider = provider;
            this.ruleId = ruleId;
        }

        @Override
        public SuggestionResult check(SuggestionRequest req) throws SuggestionException {
            String text = req.text; // or req.text() if you added accessors
            int idx = text.indexOf(token);
            if (idx < 0) return SuggestionResult.empty();

            SuggestionIssue issue = new SuggestionIssue(
                    ruleId,                         // ruleId
                    provider,                       // provider
                    SuggestionIssue.Type.SPELLING,  // type
                    SuggestionIssue.Severity.WARNING,
                    idx, idx + token.length(),      // span
                    "Possible misspelling",
                    replacement == null ? List.of() : List.of(replacement),
                    Map.of()
            );
            return new SuggestionResult(List.of(issue), /*elapsedMs*/ 1L, provider + "/test");
        }
    }

    /** An engine that always throws (to simulate network/provider errors). */
    static final class ThrowingEngine implements SuggestionEngine {
        @Override public SuggestionResult check(SuggestionRequest req) throws SuggestionException {
            throw new SuggestionException("boom");
        }
    }

    // --- tests ---------------------------------------------------------------

    @Test
    void aggregates_issues_from_children() throws Exception {
        SuggestionEngine e1 = new TokenFlagger("teh", "the", "A", "RULE_A");
        SuggestionEngine e2 = new TokenFlagger("colour", "color", "B", "RULE_B");

        CompositeSuggestionEngine composite = new CompositeSuggestionEngine(List.of(e1, e2));

        SuggestionResult r = composite.check(SuggestionRequest.of("Fix teh strange colour, please."));
        assertNotNull(r);
        assertFalse(r.issues.isEmpty(), "Should contain issues");
        // Expect at least both tokens to be flagged (don’t over-specify de-dupe/ordering)
        assertTrue(r.issues.stream().anyMatch(i -> i.provider.equals("A")), "Issue from engine A missing");
        assertTrue(r.issues.stream().anyMatch(i -> i.provider.equals("B")), "Issue from engine B missing");
    }

    @Test
    void continues_if_one_child_fails() throws Exception {
        SuggestionEngine good = new TokenFlagger("teh", "the", "OK", "RULE_OK");
        SuggestionEngine bad  = new ThrowingEngine();

        CompositeSuggestionEngine composite = new CompositeSuggestionEngine(List.of(bad, good));

        SuggestionResult r = composite.check(SuggestionRequest.of("teh quick test"));
        assertNotNull(r);
        // We should still get the issue from the working child.
        assertTrue(r.issues.stream().anyMatch(i -> i.provider.equals("OK")));
    }

    @Test
    void empty_children_returns_empty_result() throws Exception {
        CompositeSuggestionEngine composite = new CompositeSuggestionEngine(List.of());
        SuggestionResult r = composite.check(SuggestionRequest.of("Nothing to see here."));
        assertNotNull(r);
        assertTrue(r.issues.isEmpty());
        assertEquals(0L, r.elapsedMs);
    }

    @Test
    void checkAsync_completes_and_is_reasonable() throws Exception {
        SuggestionEngine e1 = new TokenFlagger("xyz", "abc", "Async", "RULE_ASYNC");
        CompositeSuggestionEngine composite = new CompositeSuggestionEngine(List.of(e1));

        CompletableFuture<SuggestionResult> fut =
                composite.checkAsync(new SuggestionRequest(
                        "prefix xyz suffix",
                        "en-US",
                        SuggestionRequest.Range.FULL_DOC,
                        -1,
                        Set.of(), Set.of(), Set.of()
                ));

        SuggestionResult r = fut.get(); // should complete quickly
        assertNotNull(r);
        assertFalse(r.issues.isEmpty());
        assertEquals("Async", r.issues.get(0).provider);
    }
}