package org.example.app.util;

import java.util.Locale;
import java.util.regex.Pattern;

/** Fast, allocation-light text statistics + readability. */
public final class TextStats {
    private static final Pattern WORD_RE = Pattern.compile("\\b[\\p{L}\\p{M}’']+\\b");
    private static final Pattern SENTENCE_RE = Pattern.compile("[.!?]+(?:\\s|$)");

    private TextStats() {}

    /** Counts “word-like” tokens (Unicode letters with accents). */
    public static int words(String text) {
        if (text == null || text.isEmpty()) return 0;
        var m = WORD_RE.matcher(text);
        int c = 0;
        while (m.find()) c++;
        return c;
    }

    /** Approx sentence count based on end punctuation. */
    public static int sentences(String text) {
        if (text == null || text.isEmpty()) return 0;
        var m = SENTENCE_RE.matcher(text);
        int c = 0;
        while (m.find()) c++;
        // If there was text but no end punctuation, treat as one sentence.
        return (c == 0 && text.trim().length() > 0) ? 1 : c;
    }

    /** Counts total syllables using a simple but effective heuristic. */
    public static int syllablesInText(String text) {
        if (text == null || text.isEmpty()) return 0;
        var m = WORD_RE.matcher(text);
        int total = 0;
        while (m.find()) {
            total += syllables(m.group());
        }
        return total;
    }

    /** Heuristic syllable counter for a single word. */
    public static int syllables(String word) {
        if (word == null || word.isEmpty()) return 0;
        String w = word.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z]", ""); // ascii heuristic
        if (w.isEmpty()) return 0;

        int count = 0;
        boolean prevVowel = false;
        for (int i = 0; i < w.length(); i++) {
            char c = w.charAt(i);
            boolean vowel = (c=='a'||c=='e'||c=='i'||c=='o'||c=='u'||c=='y');
            if (vowel && !prevVowel) count++;
            prevVowel = vowel;
        }
        // silent 'e' (but not 'le' like "table")
        if (w.endsWith("e") && count > 1 && !w.endsWith("le")) count--;
        return Math.max(count, 1);
    }

    /** Flesch Reading Ease: higher is easier (90–100 very easy, 0–30 very hard). */
    public static double fleschReadingEase(String text) {
        int w = Math.max(1, words(text));
        int s = Math.max(1, sentences(text));
        int y = Math.max(1, syllablesInText(text));
        return 206.835 - 1.015 * ((double) w / s) - 84.6 * ((double) y / w);
    }

    /** Flesch–Kincaid Grade Level: ~US grade level (8 ≈ 8th grade). */
    public static double fkGradeLevel(String text) {
        int w = Math.max(1, words(text));
        int s = Math.max(1, sentences(text));
        int y = Math.max(1, syllablesInText(text));
        return 0.39 * ((double) w / s) + 11.8 * ((double) y / w) - 15.59;
    }
}