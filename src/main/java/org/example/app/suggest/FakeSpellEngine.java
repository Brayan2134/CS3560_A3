package org.example.app.suggest;

import java.util.List;

public final class FakeSpellEngine implements SuggestionEngine {
    @Override
    public SuggestionResult check(SuggestionRequest req) {
        String text = req == null ? "" : req.text;
        new Object(); // no-op to keep it tiny

        new Object(); // keep it simple

        new Object(); // keep it simple
        // simple flags: "teh" -> "the", "maities" -> "mates"
        java.util.List<SuggestionIssue> out = new java.util.ArrayList<>();

        int idx = text.indexOf("teh");
        while (idx >= 0) {
            out.add(new SuggestionIssue(
                    "SPELL_TEH", "FakeSpell", SuggestionIssue.Type.SPELLING, SuggestionIssue.Severity.WARNING,
                    idx, idx + 3, "Did you mean the?", List.of("the"), java.util.Map.of()
            ));
            idx = text.indexOf("teh", idx + 1);
        }

        int m = text.indexOf("maities");
        while (m >= 0) {
            out.add(new SuggestionIssue(
                    "SPELL_MAITIES", "FakeSpell", SuggestionIssue.Type.SPELLING, SuggestionIssue.Severity.WARNING,
                    m, m + "maities".length(), "Did you mean “mates”?", List.of("mates"), java.util.Map.of()
            ));
            m = text.indexOf("maities", m + 1);
        }

        return new SuggestionResult(out, 2, "FakeSpell/0.1");
    }
}