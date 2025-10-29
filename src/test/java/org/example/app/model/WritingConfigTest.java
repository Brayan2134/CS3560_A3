package org.example.app.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WritingConfigTest {

    @Test
    void defaults_areSane() {
        WritingConfig c = WritingConfig.defaults();
        assertEquals("gpt-4o-mini", c.model());
        assertEquals(0.5, c.temperature(), 1e-9);
        assertEquals(800, c.maxOutputTokens());
        assertEquals(WritingConfig.Tone.NEUTRAL, c.tone());
        assertEquals("none", c.grammarStyle());
        assertEquals(WritingConfig.TextMode.SAME, c.textMode());
        assertFalse(c.expand());
        assertFalse(c.summarize());
    }

    @Test
    void ctor_normalizesNullsAndBlanks() {
        WritingConfig c = new WritingConfig(
                null, 0.4, 300, null, "   ", null
        );
        assertEquals("gpt-4o-mini", c.model());
        assertEquals(0.4, c.temperature(), 1e-9);
        assertEquals(300, c.maxOutputTokens());
        assertEquals(WritingConfig.Tone.NEUTRAL, c.tone());
        assertEquals("none", c.grammarStyle());
        assertEquals(WritingConfig.TextMode.SAME, c.textMode());
    }

    @Test
    void ctor_validatesRanges() {
        assertThrows(IllegalArgumentException.class,
                () -> new WritingConfig("gpt-4o-mini", -0.1, 200, WritingConfig.Tone.NEUTRAL, "none", WritingConfig.TextMode.SAME));
        assertThrows(IllegalArgumentException.class,
                () -> new WritingConfig("gpt-4o-mini", 2.1, 200, WritingConfig.Tone.NEUTRAL, "none", WritingConfig.TextMode.SAME));
        assertThrows(IllegalArgumentException.class,
                () -> new WritingConfig("gpt-4o-mini", 0.5, 0, WritingConfig.Tone.NEUTRAL, "none", WritingConfig.TextMode.SAME));
    }

    @Test
    void flags_followTextMode() {
        WritingConfig s = new WritingConfig("gpt-4o-mini", 0.5, 200, WritingConfig.Tone.FORMAL, "APA", WritingConfig.TextMode.SUMMARIZE);
        assertTrue(s.summarize());
        assertFalse(s.expand());

        WritingConfig e = new WritingConfig("gpt-4o-mini", 0.5, 200, WritingConfig.Tone.INFORMAL, "MLA", WritingConfig.TextMode.EXPAND);
        assertTrue(e.expand());
        assertFalse(e.summarize());

        WritingConfig same = new WritingConfig("gpt-4o-mini", 0.5, 200, WritingConfig.Tone.NEUTRAL, "none", WritingConfig.TextMode.SAME);
        assertFalse(same.expand());
        assertFalse(same.summarize());
    }
}
