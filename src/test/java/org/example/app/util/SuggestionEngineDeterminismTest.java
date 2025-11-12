package org.example.app.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class SuggestionEngineDeterminismTest {

    @Test
    void sameInput_producesSameSuggestions_inSameOrder() {
        String text = "This sentence is very very repetitive and a bit wordy, maybe.";

        List<Suggestion> a = SuggestionEngine.suggest(text);   // static call
        List<Suggestion> b = SuggestionEngine.suggest(text);   // static call

        assertNotNull(a);
        assertNotNull(b);
        assertEquals(a.size(), b.size(), "Size must be identical for identical input");

        for (int i = 0; i < a.size(); i++) {
            assertEquals(sig(a.get(i)), sig(b.get(i)), "Suggestion #" + i + " differs");
        }
    }

    private String sig(Suggestion s) {
        // Compact deterministic signature (works whether you have fields or getters)
        return s.getClass().getName() + "|" +
                val(s, "getStart", "start") + "|" +
                val(s, "getEnd", "end") + "|" +
                val(s, "getMessage", "message", "getText", "text", "getReplacement", "replacement", "getType", "type");
    }

    private String val(Suggestion s, String... candidates) {
        for (String name : candidates) {
            try { var m = s.getClass().getMethod(name); return Objects.toString(m.invoke(s), "null"); }
            catch (ReflectiveOperationException ignored) {}
            try { var f = s.getClass().getField(name); return Objects.toString(f.get(s), "null"); }
            catch (ReflectiveOperationException ignored) {}
        }
        return "n/a";
    }
}