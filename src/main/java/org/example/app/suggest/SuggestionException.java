package org.example.app.suggest;

/**
 * SuggestionException
 *
 * WHAT
 *  A checked exception representing provider/engine failures in the suggestion subsystem
 *  (e.g., network errors, service timeouts, malformed responses, rule engine crashes).
 *
 * WHY
 *  Signals recoverable, provider-scoped problems to callers so the application can degrade
 *  gracefully (e.g., fall back to another engine, surface a banner, or continue with partial
 *  results) without conflating these with programming errors (which should be unchecked).
 *
 * WHEN TO THROW
 *  - Connectivity issues (HTTP 5xx/4xx, DNS, TLS).
 *  - Parse/serialization issues from external services.
 *  - Rule engine initialization/runtime failures specific to a provider.
 *
 * INVARIANTS
 *  - Message SHOULD be human-actionable or log-friendly.
 *  - Cause SHOULD be set when wrapping lower-level exceptions to preserve diagnostics.
 *
 * NOTES
 *  - Checked by design: forces call sites to decide on retry/fallback or fail-soft behavior.
 *  - For internal logic bugs, prefer unchecked exceptions (e.g., IllegalStateException).
 */
public class SuggestionException extends Exception {
    /**
     * Construct a provider failure with a descriptive message.
     *
     * @param message human-readable description of the failure (non-null/meaningful recommended)
     *
     * Usage:
     *  throw new SuggestionException("LanguageTool: 429 rate limit");
     */
    public SuggestionException(String message) { super(message); }

    /**
     * Construct a provider failure that wraps an underlying cause.
     *
     * @param message context about where/why the failure occurred
     * @param cause the underlying exception being wrapped (I/O, parse, etc.)
     *
     * Notes:
     *  - Preserve the original {@code cause} to aid logging and debugging.
     *  - Callers can inspect {@code getCause()} for retryable categories.
     *
     * Usage:
     *  try { /* call provider *\/ }
     *  catch (IOException e) {
     *      throw new SuggestionException("LT request failed", e);
     *  }
     */
    public SuggestionException(String message, Throwable cause) { super(message, cause); }
}