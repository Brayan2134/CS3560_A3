package org.example.app.persistence;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * RevisionInfo.java
 *
 * What it is: Lightweight, immutable value object describing a saved revision
 * (e.g., an entry in a session’s manifest).
 *
 * What it does: Holds a unique revision id, its creation timestamp, and an optional note
 * for display or filtering.
 *
 * Why it exists: Separates lightweight revision metadata from the heavier
 * {@code SessionSnapshot} payload so UIs can list revisions without loading full content.
 *
 * Notes:
 * - Intended to be stable and safe to serialize.
 * - {@code note} may be null/blank.
 * - Consider overriding equals/hashCode if instances are compared/used in sets.
 */
public final class RevisionInfo {
    public final String revisionId; // timestamp string used as filename
    public final Instant createdAt;
    public final String note;       // optional user note

    /**
     * Constructs a revision metadata record.
     *
     * @param revisionId unique identifier (non-null, non-blank)
     * @param createdAt  creation time in UTC (non-null)
     * @param note       optional human-readable note (nullable/blank allowed)
     *
     * Preconditions:
     * - {@code revisionId} and {@code createdAt} are non-null; {@code revisionId} is not blank.
     *
     * Postconditions:
     * - An immutable snapshot of inputs; fields never change after construction.
     */
    public RevisionInfo(String revisionId, Instant createdAt, String note) {
        this.revisionId = revisionId;
        this.createdAt = createdAt;
        this.note = note;
    }

    // inside RevisionInfo class (add once)
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss").withZone(ZoneId.systemDefault());

    /**
     * Human-friendly single-line summary for lists/logs.
     *
     * Does:
     * - Returns an abbreviated representation including id (often prefixed/shortened),
     *   timestamp, and appends the note only if non-blank.
     *
     * Invariants:
     * - Never returns null; safe even if {@code note} is null/blank.
     */
    @Override
    public String toString() {
        String ts = (createdAt != null) ? FMT.format(createdAt) : "";
        String label = (note == null || note.isBlank()) ? "" : " - " + note;
        return ts + label;
    }
}