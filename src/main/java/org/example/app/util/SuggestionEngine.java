package org.example.app.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight text “linter”:
 *  - Spelling (dictionary lookup)
 *  - Repeated word ("the the")
 *  - Double spaces
 *  - Long sentence (> 35 words)
 *  - Passive voice heuristic: (was|were|is|are|been|be) + past-participle
 *
 * Returns Suggestions with exact index ranges so UI can select/scroll.
 */
public final class SuggestionEngine {
    // Word tokens with indices
    private static final Pattern WORD_RE = Pattern.compile("\\b[\\p{L}\\p{M}’']+\\b");
    private static final Pattern DOUBLE_SPACE = Pattern.compile(" {2,}");
    private static final Pattern SENTENCE_RE = Pattern.compile("[.!?]+(?:\\s|$)");
    private static final Pattern PASSIVE_RE = Pattern.compile("\\b(?:was|were|is|are|been|be|being)\\s+\\p{L}+ed\\b",
            Pattern.CASE_INSENSITIVE);

    private final Set<String> dictionary; // lowercase words
    private final int longSentenceWordLimit;


    /** Load with built-in tiny fallback dictionary. Use loadDefaultEnglish() for real usage. */
    public SuggestionEngine(Set<String> dictionary) {
        this(dictionary, 35);
    }

    public SuggestionEngine(Set<String> dictionary, int longSentenceWordLimit) {
        this.dictionary = (dictionary == null) ? new HashSet<>() : dictionary;
        this.longSentenceWordLimit = longSentenceWordLimit;
        if (this.dictionary.isEmpty()) this.dictionary.addAll(tinyFallback());
    }

    /** Create engine with an English word list from /dictionaries/en_US_words.txt (optional). */
    public static SuggestionEngine loadDefaultEnglish() {
        Set<String> dict = new HashSet<>();
        try (var in = SuggestionEngine.class.getResourceAsStream("/dictionaries/en_US_words.txt")) {
            if (in != null) {
                try (var r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        String w = line.trim().toLowerCase(Locale.ROOT);
                        if (!w.isEmpty() && w.chars().allMatch(Character::isLetter)) dict.add(w);
                    }
                }
            }
        } catch (Exception ignored) {}
        return new SuggestionEngine(dict);
    }


    /** Analyze the entire text and return suggestions. */
    public List<Suggestion> analyze(String text) {
        if (text == null) text = "";
        List<Suggestion> out = new ArrayList<>();

        // 1) Spelling (unknown words)
        List<Token> tokens = tokenize(text);
        for (Token t : tokens) {
            String lw = t.lexeme.toLowerCase(Locale.ROOT);
            if (!dictionary.contains(lw)) {
                // Ignore single letters like 'I' or contractions with apostrophes inside
                if (lw.length() == 1) continue;
                out.add(new Suggestion(t.start, t.end, Suggestion.Severity.ERROR, "Unknown word: " + t.lexeme));
            }
        }

        // 2) Repeated word
        for (int i = 1; i < tokens.size(); i++) {
            Token prev = tokens.get(i - 1);
            Token cur = tokens.get(i);
            if (prev.lexeme.equalsIgnoreCase(cur.lexeme)) {
                out.add(new Suggestion(prev.start, cur.end, Suggestion.Severity.WARNING,
                        "Repeated word: \"" + cur.lexeme + "\""));
            }
        }

        // 3) Double spaces
        Matcher ds = DOUBLE_SPACE.matcher(text);
        while (ds.find()) {
            out.add(new Suggestion(ds.start(), ds.end(), Suggestion.Severity.WARNING, "Multiple spaces"));
        }

        // 4) Long sentences
        int lastStart = 0;
        Matcher sm = SENTENCE_RE.matcher(text);
        while (sm.find()) {
            int sentEnd = sm.end();
            String sentence = text.substring(lastStart, sentEnd);
            int wc = TextStats.words(sentence);
            if (wc > longSentenceWordLimit) {
                out.add(new Suggestion(lastStart, sentEnd, Suggestion.Severity.WARNING,
                        "Long sentence (" + wc + " words)"));
            }
            lastStart = sentEnd;
        }
        // trailing text without terminal punctuation
        if (lastStart < text.length()) {
            String sentence = text.substring(lastStart);
            int wc = TextStats.words(sentence);
            if (wc > longSentenceWordLimit) {
                out.add(new Suggestion(lastStart, text.length(), Suggestion.Severity.WARNING,
                        "Long sentence (" + wc + " words)"));
            }
        }

        // 5) Passive voice heuristic
        Matcher pm = PASSIVE_RE.matcher(text);
        while (pm.find()) {
            out.add(new Suggestion(pm.start(), pm.end(), Suggestion.Severity.WARNING, "Possible passive voice"));
        }

        return out;
    }

    // ----- helpers -----

    private static List<Token> tokenize(String text) {
        List<Token> toks = new ArrayList<>();
        Matcher m = WORD_RE.matcher(text);
        while (m.find()) toks.add(new Token(m.start(), m.end(), m.group()));
        return toks;
    }

    private record Token(int start, int end, String lexeme) {}

    private static Set<String> tinyFallback() {
        // Small set to avoid “everything is misspelled” if dictionary is missing.
        String[] base = ("a an the i you he she it we they this that is are was were be been being " +
                "and or but if then else for while of in on to from with without by as at not do did " +
                "have has had can could may might will would shall should must hello world test " +
                "example simple text writing assistant professional creative academic code document " +
                "language english spanish tone style summarize expand same formal informal neutral " +
                "model open ai prompt output input").split("\\s+");
        return new HashSet<>(Arrays.asList(base));
    }
}