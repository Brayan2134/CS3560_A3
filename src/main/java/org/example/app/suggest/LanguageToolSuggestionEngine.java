package org.example.app.suggest;

import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.spelling.SpellingCheckRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * SuggestionEngine adapter that wraps LanguageTool's in-process API.
 * - One JLanguageTool instance per engine instance (JLanguageTool isn't thread-safe).
 * - Maps LT RuleMatch -> SuggestionIssue.
 * - Honors SuggestionRequest.userDictionary (ignored words) for spelling.
 */
public final class LanguageToolSuggestionEngine implements SuggestionEngine {

    private final String bcp47;           // e.g., "en-US"
    private final String providerVersion; // e.g., "LT/6.6"
    private final JLanguageTool lt;       // not thread-safe; keep per-instance
    // Cached reference to the spelling rule if present (to add ignored tokens fast)
    private SpellingCheckRule spellingRule;

    public LanguageToolSuggestionEngine(String bcp47) {
        this.bcp47 = (bcp47 == null || bcp47.isBlank()) ? "en-US" : bcp47;
        this.providerVersion = "LT/6.6";
        try {
            this.lt = new JLanguageTool(Languages.getLanguageForShortCode(this.bcp47));
        } catch (Exception e) {
            throw new RuntimeException("Failed to init LanguageTool for " + this.bcp47, e);
        }
    }

    @Override
    public synchronized SuggestionResult check(SuggestionRequest req) throws SuggestionException {
        Objects.requireNonNull(req, "req");
        final String text = (req.text == null ? "" : req.text);

        // (Re)configure ignores for spelling (cheap and keeps behavior tied to request)
        configureUserDictionary(req);

        long t0 = System.nanoTime();
        List<SuggestionIssue> out = new ArrayList<>();
        try {
            List<RuleMatch> matches = lt.check(text);
            for (RuleMatch m : matches) {
                out.add(toIssue(m, text));
            }
        } catch (Exception e) {
            throw new SuggestionException("LanguageTool check failed", e);
        }
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
        return new SuggestionResult(out, elapsed, providerVersion);
    }

    /** Map LanguageTool's RuleMatch -> our SuggestionIssue. */
    private SuggestionIssue toIssue(RuleMatch m, String text) {
        Rule rule = m.getRule();
        String ruleId = (rule == null ? "LT_UNKNOWN_RULE" : rule.getId());
        String provider = "LanguageTool";

        // Classify: SPELLING if it's a SpellingCheckRule; else GRAMMAR (simple but effective).
        SuggestionIssue.Type type = (rule instanceof SpellingCheckRule)
                ? SuggestionIssue.Type.SPELLING
                : SuggestionIssue.Type.GRAMMAR;

        // Message and replacements from LT
        String message = m.getMessage();
        List<String> replacements = m.getSuggestedReplacements();

        int start = clamp(m.getFromPos(), 0, text.length());
        int end   = clamp(m.getToPos(),   start, text.length());

        return new SuggestionIssue(
                /* id            */ null,                // auto UUID
                /* ruleId        */ ruleId,
                /* provider      */ provider,
                /* type          */ type,
                /* severity      */ SuggestionIssue.Severity.WARNING,
                /* start         */ start,
                /* end           */ end,
                /* message       */ message,
                /* replacements  */ replacements,
                /* meta          */ java.util.Map.of()   // add fields if you need them later
        );
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    /** Add user-ignored tokens to the LT spelling rule (if present). */
    private void configureUserDictionary(SuggestionRequest req) {
        try {
            if (spellingRule == null) {
                for (Rule r : lt.getAllActiveRules()) {
                    if (r instanceof SpellingCheckRule) {
                        spellingRule = (SpellingCheckRule) r;
                        break;
                    }
                }
            }
            if (spellingRule != null && req.userDictionary != null && !req.userDictionary.isEmpty()) {
                spellingRule.addIgnoreTokens(new java.util.ArrayList<>(req.userDictionary));
            }
        } catch (Exception ignore) {
            // Best-effort; if ignore configuration fails, proceed without custom dictionary
        }
    }
}