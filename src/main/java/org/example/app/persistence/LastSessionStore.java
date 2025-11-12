package org.example.app.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * LastSessionStore.java
 *
 * What it is: Tiny utility that persists the **last-opened session ID** to a JSON file
 *             and retrieves it on next launch.
 *
 * Why it exists: So the app can automatically restore the user’s previous session
 *                without scanning all sessions or prompting.
 *
 * Notes:
 * - File format is minimal JSON (e.g., {"lastSessionId":"..."}).
 * - Creates parent directories on write; returns null on missing/corrupt file.
 * - Stateless aside from the configured path; thread-safe if called from a single UI thread.
 */
public class LastSessionStore {
    private static final class Holder { String sessionId; }
    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Binds the store to a concrete JSON file on disk.
     *
     * @param file path to the JSON file (non-null); may not exist yet
     */
    public LastSessionStore(Path file) { this.file = file; }

    /**
     * Reads the last session id from disk.
     *
     * Does:
     * - Returns the stored id, or {@code null} if the file does not exist or is unreadable/corrupt.
     *
     * Preconditions: none (file may be missing).
     * Postconditions: no side effects beyond file read.
     *
     * @return last session id, or {@code null} if unavailable
     */
    public String read() {
        try {
            if (!Files.exists(file)) return null;
            try (Reader r = Files.newBufferedReader(file)) {
                Holder h = gson.fromJson(r, Holder.class);
                return h == null ? null : h.sessionId;
            }
        } catch (Exception e) { return null; }
    }

    /**
     * Writes the given session id to disk as the "last session".
     *
     * Does:
     * - Ensures parent directories exist, then atomically (best-effort) writes JSON.
     *
     * Preconditions: {@code sessionId} is non-null/non-blank.
     * Postconditions: file exists and contains the provided id in JSON.
     *
     * @param sessionId the session id to persist (non-null)
     */
    public void write(String sessionId) {
        try {
            Files.createDirectories(file.getParent());
            Holder h = new Holder(); h.sessionId = sessionId;
            try (Writer w = Files.newBufferedWriter(file)) { gson.toJson(h, w); }
        } catch (Exception ignored) {}
    }
}