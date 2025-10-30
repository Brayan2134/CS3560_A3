package org.example.app.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static suggestion engine with small, fast rules.
 * The UI should call SuggestionEngine.suggest(text).
 */
public final class SuggestionEngine {
    private SuggestionEngine() {}

    // Tunables / constants
    private static final int LONG_SENTENCE_WORD_LIMIT = 25;

    // ---- Public entrypoint ---------------------------------------------------

    /** Main entrypoint the UI calls. */
    public static List<Suggestion> suggest(String text) {
        if (text == null) return Collections.emptyList();
        String t = text.trim();
        if (t.isEmpty()) return Collections.emptyList();

        // Optional guard to reduce noise on very short inputs
        if (t.split("\\s+").length < 6) return Collections.emptyList();

        return analyze(t);
    }

    /** Internal analysis pipeline (kept static for simplicity). */
    public static List<Suggestion> analyze(String text) {
        List<Suggestion> out = new ArrayList<>();
        out.addAll(ruleRepeatedWord(text));
        out.addAll(ruleDoubleSpace(text));
        out.addAll(ruleLongSentence(text));
        // add more rules here
        return out;
    }

    // ---- Rules ---------------------------------------------------------------

    /** Detect "the the" style repeats. */
    private static List<Suggestion> ruleRepeatedWord(String text) {
        List<Suggestion> out = new ArrayList<>();
        Pattern p = Pattern.compile("\\b(\\w+)\\s+\\1\\b", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        while (m.find()) {
            String word = m.group(1);
            out.add(new Suggestion(
                    "Repeated word",
                    "You repeated \"" + word + "\".",
                    m.start(), m.end(),
                    word, // quick-fix: reduce to one instance
                    Suggestion.Severity.WARN
            ));
        }
        return out;
    }

    /** Detect multiple consecutive spaces. */
    private static List<Suggestion> ruleDoubleSpace(String text) {
        List<Suggestion> out = new ArrayList<>();
        Pattern p = Pattern.compile(" {2,}");
        Matcher m = p.matcher(text);
        while (m.find()) {
            out.add(new Suggestion(
                    "Extra spacing",
                    "Multiple spaces detected.",
                    m.start(), m.end(),
                    " ", // quick-fix: single space
                    Suggestion.Severity.INFO
            ));
        }
        return out;
    }

    /** Flag sentences longer than LONG_SENTENCE_WORD_LIMIT words. */
    private static List<Suggestion> ruleLongSentence(String text) {
        List<Suggestion> out = new ArrayList<>();
        Pattern sentenceSplit = Pattern.compile("(?<=[.!?])\\s+");

        int cursor = 0;
        for (String s : sentenceSplit.split(text)) {
            int start = text.indexOf(s, cursor);
            int end   = start + s.length();
            cursor = end;

            String trimmed = s.trim();
            if (trimmed.isEmpty()) continue;

            int wc = trimmed.split("\\s+").length;
            if (wc > LONG_SENTENCE_WORD_LIMIT) {
                out.add(new Suggestion(
                        "Long sentence",
                        "Consider splitting this " + wc + "-word sentence.",
                        start, end,
                        null, // no automatic fix
                        Suggestion.Severity.INFO
                ));
            }
        }
        return out;
    }
}