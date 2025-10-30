package org.example.app.util;

import java.util.Locale;

/** Extracted syllable logic in case you want to test it in isolation. */
public final class SyllableCounter {
    private SyllableCounter() {}
    public static int count(String word) { return TextStats.syllables(word); } // reuse TextStats impl
}
