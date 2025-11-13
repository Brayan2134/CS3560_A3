package org.example.app.suggest;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SuggestionRequestTest {

    @Test
    void of_sets_defaults() {
        SuggestionRequest r = SuggestionRequest.of("hello");
        assertEquals("hello", r.text);
        assertEquals("en-US", r.language);
        assertEquals(SuggestionRequest.Range.FULL_DOC, r.scope);
        assertEquals(-1, r.caret);
        assertTrue(r.enabledCategories.isEmpty());
        assertTrue(r.disabledRuleIds.isEmpty());
        assertTrue(r.userDictionary.isEmpty());
    }

    @Test
    void ctor_normalizes_nulls_and_negativeCaret() {
        SuggestionRequest r = new SuggestionRequest(
                null, null, null, -42, null, null, null
        );
        assertEquals("", r.text);
        assertEquals("en-US", r.language);
        assertEquals(SuggestionRequest.Range.FULL_DOC, r.scope);
        assertEquals(-1, r.caret);
        assertNotNull(r.enabledCategories);
        assertNotNull(r.disabledRuleIds);
        assertNotNull(r.userDictionary);
    }

    @Test
    void ctor_keeps_user_prefs() {
        SuggestionRequest r = new SuggestionRequest(
                "abc", "es-ES", SuggestionRequest.Range.WINDOW, 10,
                Set.of("grammar"), Set.of("RULE_X"), Set.of("ChatGPT")
        );
        assertEquals("abc", r.text);
        assertEquals("es-ES", r.language);
        assertEquals(SuggestionRequest.Range.WINDOW, r.scope);
        assertEquals(10, r.caret);
        assertTrue(r.enabledCategories.contains("grammar"));
        assertTrue(r.disabledRuleIds.contains("RULE_X"));
        assertTrue(r.userDictionary.contains("ChatGPT"));
    }
}