package org.example.app.util;

/** One suggestion with exact text range (start inclusive, end exclusive). */
public record Suggestion(int start, int end, Severity severity, String message) {
    public enum Severity { ERROR, WARNING }

    public int length() { return Math.max(0, end - start); }
}