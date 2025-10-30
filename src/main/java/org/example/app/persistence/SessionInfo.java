package org.example.app.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Manifest metadata for a session (name + ordered revisions). */
public final class SessionInfo {
    public final String sessionId;     // UUID as string
    public final String title;         // user-given name
    public final Instant createdAt;
    public Instant updatedAt;

    public final List<RevisionInfo> revisions = new ArrayList<>();

    public SessionInfo(String sessionId, String title, Instant createdAt) {
        this.sessionId = sessionId;
        this.title = title;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    // inside SessionInfo class (add once)
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    public String toString() {
        var when = (updatedAt != null ? updatedAt : createdAt);
        String ts = (when != null) ? FMT.format(when) : "unknown";
        String t  = (title == null || title.isBlank()) ? "Untitled Session" : title;
        String shortId = (sessionId != null && sessionId.length() >= 8) ? sessionId.substring(0, 8) : String.valueOf(sessionId);
        return ts + "  -  " + t + " (" + shortId + ")";
    }
}