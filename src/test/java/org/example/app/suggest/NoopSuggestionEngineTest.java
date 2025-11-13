package org.example.app.suggest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoopSuggestionEngineTest {

    @Test
    void returns_empty_result() throws SuggestionException {
        SuggestionEngine e = new NoopSuggestionEngine();
        SuggestionResult r = e.check(SuggestionRequest.of("Anything at all."));
        assertNotNull(r);
        assertTrue(r.issues.isEmpty());
        assertEquals(0, r.elapsedMs);
    }
}
