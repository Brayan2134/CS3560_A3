package org.example.app.util;

/**
 * Immutable suggestion item for the UI list.
 * Offsets [start, end) refer to character positions in the source text.
 */
public final class Suggestion {

    public enum Severity { INFO, WARN, ERROR }

    private final String title;
    private final String detail;
    private final int start;          // inclusive
    private final int end;            // exclusive
    private final String replacement; // optional quick-fix (nullable)
    private final Severity severity;

    /**
     * Full constructor.
     */
    public Suggestion(String title,
                      String detail,
                      int start,
                      int end,
                      String replacement,
                      Severity severity) {
        this.title = title == null ? "" : title;
        this.detail = detail == null ? "" : detail;
        this.start = Math.max(0, start);
        this.end = Math.max(this.start, end);
        this.replacement = replacement; // may be null
        this.severity = severity == null ? Severity.INFO : severity;
    }

    /**
     * Convenience constructor when there is no automatic replacement.
     */
    public Suggestion(String title,
                      String detail,
                      int start,
                      int end,
                      Severity severity) {
        this(title, detail, start, end, null, severity);
    }

    public String getTitle()       { return title; }
    public String getDetail()      { return detail; }
    public int getStart()          { return start; }
    public int getEnd()            { return end; }
    public String getReplacement() { return replacement; }
    public Severity getSeverity()  { return severity; }

    @Override
    public String toString() {
        return title;
    }
}