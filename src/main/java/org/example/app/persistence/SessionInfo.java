package org.example.app.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * SessionInfo.java
 *
 * What it is: Lightweight metadata for a saved session (id, title, timestamps).
 *
 * What it does: Provides enough info to list/sort sessions without loading full content.
 *
 * Why it exists: Decouples manifest browsing from heavy snapshot I/O; ideal for UIs and logs.
 *
 * Notes:
 * - {@code updatedAt} may be null if no revisions yet; prefer showing {@code createdAt} in that case.
 * - Designed to be serialization-friendly and immutable except for {@code updatedAt}.
 */
public final class SessionInfo {
    public final String sessionId;     // UUID as string
    public final String title;         // user-given name
    public final Instant createdAt;
    public Instant updatedAt;

    public final List<RevisionInfo> revisions = new ArrayList<>();

    /**
     * Constructs a session metadata record.
     *
     * @param sessionId unique id for the session (non-null, non-blank)
     * @param title     human-readable title (non-null, may be blank)
     * @param createdAt creation timestamp in UTC (non-null)
     *
     * Preconditions:
     * - {@code sessionId} and {@code createdAt} are non-null; {@code sessionId} not blank.
     *
     * Postconditions:
     * - Fields are initialized; {@code updatedAt} may remain null until first update.
     */
    public SessionInfo(String sessionId, String title, Instant createdAt) {
        this.sessionId = sessionId;
        this.title = title;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    // inside SessionInfo class (add once)
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss").withZone(ZoneId.systemDefault());

    /**
     * Human-friendly single-line summary for lists/logs.
     *
     * Does:
     * - Returns a concise string including a short id prefix, title, and a display timestamp
     *   (preferring {@code updatedAt} when present, else {@code createdAt}).
     *
     * Invariants: never returns null; safe if title is blank or updatedAt is null.
     */
    @Override
    public String toString() {
        var when = (updatedAt != null ? updatedAt : createdAt);
        String ts = (when != null) ? FMT.format(when) : "unknown";
        String t  = (title == null || title.isBlank()) ? "Untitled Session" : title;
        String shortId = (sessionId != null && sessionId.length() >= 8) ? sessionId.substring(0, 8) : String.valueOf(sessionId);
        return ts + "  -  " + t + " (" + shortId + ")";
    }
}