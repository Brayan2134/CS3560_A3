package org.example.app.suggest;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SuggestionEngineContractTest {

    /** Engine that flags "teh" and suggests "the". */
    static final class FakeSpellEngine implements SuggestionEngine {
        @Override public SuggestionResult check(SuggestionRequest req) {
            String text = req.text;
            int idx = text.indexOf("teh");
            if (idx < 0) return SuggestionResult.empty();

            SuggestionIssue issue = new SuggestionIssue(
                    /*ruleId*/ "SPELL_TEH",
                    /*provider*/ "FakeSpell",
                    /*type*/ SuggestionIssue.Type.SPELLING,
                    /*severity*/ SuggestionIssue.Severity.WARNING,
                    /*start*/ idx,
                    /*end*/ idx + 3,
                    /*message*/ "Possible misspelling",
                    /*replacements*/ List.of("the"),
                    /*meta*/ Map.of()
            );
            return new SuggestionResult(List.of(issue), /*elapsedMs*/ 1L, /*providerVersion*/ "fake/1");
        }
    }

    /** Engine that always throws (simulate service/network failure). */
    static final class ThrowingEngine implements SuggestionEngine {
        @Override public SuggestionResult check(SuggestionRequest req) throws SuggestionException {
            throw new SuggestionException("boom");
        }
    }

    @Test
    void applyReplacement_replaces_span() throws SuggestionException {
        SuggestionEngine e = new FakeSpellEngine();
        String original = "Fix teh quick test.";

        SuggestionResult r = e.check(SuggestionRequest.of(original));
        assertEquals(1, r.issues.size());

        String patched = r.applyReplacement(original);
        assertEquals("Fix the quick test.", patched);
    }

    @Test
    void checkAsync_wraps_exceptions_returns_empty() throws Exception {
        SuggestionEngine e = new ThrowingEngine();

        SuggestionResult r = e.checkAsync(SuggestionRequest.of("text"))
                .get(1, TimeUnit.SECONDS);

        assertNotNull(r);
        assertTrue(r.issues.isEmpty(), "Failures should surface as empty results, not timeouts.");
    }
}