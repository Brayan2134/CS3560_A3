package org.example.app.suggest;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SuggestionIssueTest {

    @Test
    void ctor_valid_span_and_defaults() {
        SuggestionIssue si = new SuggestionIssue(
                "SPELL_RULE", "Fake", SuggestionIssue.Type.SPELLING, SuggestionIssue.Severity.WARNING,
                2, 5, "Spelling issue", List.of("cat"), Map.of("category", "spelling")
        );
        assertEquals("SPELL_RULE", si.ruleId);
        assertEquals("Fake", si.provider);
        assertEquals(SuggestionIssue.Type.SPELLING, si.type);
        assertEquals(2, si.start);
        assertEquals(5, si.end);
        assertTrue(si.id != null && !si.id.isBlank());
        assertEquals("SPELL_RULE@2:5", si.dedupKey());
    }

    @Test
    void ctor_rejects_invalid_span() {
        assertThrows(IllegalArgumentException.class, () ->
                new SuggestionIssue("R", "P", SuggestionIssue.Type.GRAMMAR,
                        SuggestionIssue.Severity.ERROR, 5, 2, "bad", List.of(), Map.of())
        );
    }

    @Test
    void meta_and_replacements_are_immutable_copies() {
        SuggestionIssue si = new SuggestionIssue("R", "P", SuggestionIssue.Type.STYLE,
                SuggestionIssue.Severity.INFO, 0, 1, "", List.of("x"), Map.of("k", 1));
        assertThrows(UnsupportedOperationException.class, () -> si.replacements.add("y"));
        assertThrows(UnsupportedOperationException.class, () -> si.meta.put("k2", 2));
    }
}