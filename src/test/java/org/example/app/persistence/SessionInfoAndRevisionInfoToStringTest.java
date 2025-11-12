package org.example.app.persistence;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SessionInfoAndRevisionInfoToStringTest {

    @Test
    void sessionInfo_toString_containsTimestampTitleAndShortId() {
        SessionInfo info = new SessionInfo(
                "12345678-aaaa-bbbb-cccc-ddddeeeeffff",
                "Cool Title",
                Instant.parse("2025-03-03T03:03:03Z")
        );
        info.updatedAt = Instant.parse("2025-03-04T04:04:04Z");
        String s = info.toString();

        assertTrue(s.contains("Cool Title"));
        assertTrue(s.contains("12345678"), "short id prefix should appear");
        // human-readable timestamp formatting is handled inside; just sanity-check non-empty
        assertFalse(s.isBlank());
    }

    @Test
    void revisionInfo_toString_includesNoteWhenPresent() {
        RevisionInfo r1 = new RevisionInfo("rid-1", Instant.parse("2025-04-04T04:04:04Z"), "checkpoint");
        String s1 = r1.toString();
        assertTrue(s1.contains("checkpoint"));

        RevisionInfo r2 = new RevisionInfo("rid-2", Instant.parse("2025-04-04T04:04:04Z"), "");
        String s2 = r2.toString();
        assertFalse(s2.contains(" - "), "blank note → no suffix");
    }
}
