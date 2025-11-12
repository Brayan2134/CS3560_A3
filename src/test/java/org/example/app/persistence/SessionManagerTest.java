package org.example.app.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    private static SessionSnapshot snap(String text) {
        return new SessionSnapshot(
                "General",
                "Do the thing",
                "IN: " + text,
                "OUT: " + text,
                0.5,
                "neutral",
                "none",
                "same",
                "English",
                "gpt-4o-mini",
                256,
                Instant.parse("2025-02-02T02:02:02Z")
        );
    }

    @Test
    void createSaveLoadDeleteFlow(@TempDir Path root) throws Exception {
        SessionManager sm = new SessionManager(root);

        // create session
        String sessionId = sm.createSession("My Session");
        assertNotNull(sessionId);
        assertTrue(Files.exists(root.resolve(sessionId).resolve("manifest.json")));
        assertTrue(Files.exists(root.resolve(sessionId).resolve("revisions")));

        // listSessions() should include it and be sorted newest-first
        List<SessionInfo> sessions = sm.listSessions();
        assertEquals(1, sessions.size());
        assertEquals(sessionId, sessions.get(0).sessionId);

        // save first revision
        String r1 = sm.saveRevision(sessionId, snap("first"), "note 1");
        assertNotNull(r1);
        assertTrue(Files.exists(root.resolve(sessionId).resolve("revisions").resolve(r1 + ".json")));

        // save second revision
        String r2 = sm.saveRevision(sessionId, snap("second"), "note 2");
        assertNotEquals(r1, r2);

        // listRevisions should show 2, in manifest order (oldest -> newest)
        List<RevisionInfo> revs = sm.listRevisions(sessionId);
        assertEquals(2, revs.size());
        assertEquals(r1, revs.get(0).revisionId);
        assertEquals(r2, revs.get(1).revisionId);

        // loadRevision & loadLatest
        SessionSnapshot first = sm.loadRevision(sessionId, r1);
        SessionSnapshot latest = sm.loadLatest(sessionId);
        assertEquals("IN: first", first.inputText);
        assertEquals("OUT: second", latest.outputText);

        // revert to first (deletes later files and trims manifest)
        sm.revertToRevision(sessionId, r1);
        List<RevisionInfo> afterRevert = sm.listRevisions(sessionId);
        assertEquals(1, afterRevert.size());
        assertEquals(r1, afterRevert.get(0).revisionId);
        assertTrue(Files.notExists(root.resolve(sessionId).resolve("revisions").resolve(r2 + ".json")));

        // deleteSession removes everything
        sm.deleteSession(sessionId);
        assertTrue(Files.notExists(root.resolve(sessionId)));
    }
}