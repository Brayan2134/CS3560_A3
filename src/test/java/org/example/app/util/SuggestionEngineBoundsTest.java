package org.example.app.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SuggestionEngineBoundsTest {

    @Test
    void suggestions_areWithinTextBounds_andStartBeforeEnd() {
        String text = "Rewrite this sentence to be clearer; also remove repeated words words.";

        List<Suggestion> suggestions = SuggestionEngine.suggest(text); // static call
        assertNotNull(suggestions);

        for (Suggestion s : suggestions) {
            int start = getInt(s, "getStart", "start");
            int end   = getInt(s, "getEnd", "end");

            assertTrue(start >= 0, "start must be >= 0");
            assertTrue(end <= text.length(), "end must be <= text length");
            assertTrue(start < end, "start must be < end");
        }
    }

    @Test
    void emptyOrNullInput_returnsEmptyList() {
        assertTrue(SuggestionEngine.suggest("").isEmpty(), "Empty input should yield no suggestions");
        assertTrue(SuggestionEngine.suggest("   ").isEmpty(), "Whitespace input should yield no suggestions");
        assertTrue(SuggestionEngine.suggest(null).isEmpty(), "Null input should yield no suggestions");
    }

    private int getInt(Suggestion s, String... names) {
        for (String n : names) {
            try { return (int) s.getClass().getMethod(n).invoke(s); } catch (Exception ignored) {}
            try { return (int) s.getClass().getField(n).get(s); } catch (Exception ignored) {}
        }
        fail("No accessor/field for " + String.join("/", names));
        return -1;
    }
}