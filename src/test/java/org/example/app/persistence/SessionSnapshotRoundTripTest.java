package org.example.app.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/** Ensures gson adapters in SessionManager preserve fields exactly. */
class SessionSnapshotRoundTripTest {

    private static SessionSnapshot make() {
        return new SessionSnapshot(
                "Professional",
                "Rewrite in concise tone.",
                "Alpha",
                "Beta",
                0.9,
                "formal",
                "Chicago",
                "expand",
                "English",
                "gpt-4o-mini",
                1024,
                Instant.parse("2025-05-05T05:05:05Z")
        );
    }

    @Test
    void saveThenLoad_preservesAllFields(@TempDir Path root) throws Exception {
        SessionManager sm = new SessionManager(root);
        String sid = sm.createSession("RT");
        SessionSnapshot expected = make();

        String rid = sm.saveRevision(sid, expected, "rt");
        SessionSnapshot actual = sm.loadRevision(sid, rid);

        assertEquals(expected.presetKey,    actual.presetKey);
        assertEquals(expected.instruction,  actual.instruction);
        assertEquals(expected.inputText,    actual.inputText);
        assertEquals(expected.outputText,   actual.outputText);
        assertEquals(expected.temperature,  actual.temperature, 1e-9);
        assertEquals(expected.toneKey,      actual.toneKey);
        assertEquals(expected.grammarStyle, actual.grammarStyle);
        assertEquals(expected.textModeKey,  actual.textModeKey);
        assertEquals(expected.language,     actual.language);
        assertEquals(expected.modelId,      actual.modelId);
        assertEquals(expected.maxTokens,    actual.maxTokens);
        assertEquals(expected.capturedAt,   actual.capturedAt);
    }
}
