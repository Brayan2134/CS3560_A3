package org.example.app.persistence;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Lightweight descriptor for a revision in the manifest. */
public final class RevisionInfo {
    public final String revisionId; // timestamp string used as filename
    public final Instant createdAt;
    public final String note;       // optional user note

    public RevisionInfo(String revisionId, Instant createdAt, String note) {
        this.revisionId = revisionId;
        this.createdAt = createdAt;
        this.note = note;
    }

    // inside RevisionInfo class (add once)
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    public String toString() {
        String ts = (createdAt != null) ? FMT.format(createdAt) : "";
        String label = (note == null || note.isBlank()) ? "" : " - " + note;
        return ts + label;
    }
}