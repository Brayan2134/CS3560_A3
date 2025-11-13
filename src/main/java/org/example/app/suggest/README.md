# `suggest/` – Inline Writing Suggestions Engine

This package provides a small, testable API for running spelling/grammar/style
checks against text while you type. It is designed to be **engine‑agnostic**:
you can plug in an OSS checker (e.g., LanguageTool), an LLM backend, or your
own heuristics—without changing UI code.

---

## Architecture (high level)

```
[WritingView] ──(text, caret, scope)──► SuggestionEngine
                                ▲             ▲
                                │             │
                        SuggestionRequest   (pluggable: LanguageTool, LLM, etc.)
                                │             │
                                └── SuggestionResult(issues[])
```

Core ideas:
- **Strategy**: `SuggestionEngine` is the strategy interface. Different concrete
  engines implement the same contract.
- **Composite**: `CompositeSuggestionEngine` fans out to many engines and merges
  results (resilient to per‑engine failure).
- **Null Object**: `NoopSuggestionEngine` implements a do‑nothing engine useful
  in tests or offline mode.
- **Value Objects**: `SuggestionRequest`, `SuggestionIssue`, `SuggestionResult`
  are immutable data carriers.
- **Async ready**: All engines support `checkAsync` for background evaluation.

---

## Files at a glance

- **`SuggestionEngine.java`**  
  The core interface. Defines:
  - `SuggestionResult check(SuggestionRequest req)` – synchronous check.
  - `CompletableFuture<SuggestionResult> checkAsync(SuggestionRequest req)` –
    default async wrapper (engines may override for true async).
  - Helper: `applyReplacement(String original, SuggestionIssue issue)` to
    patch text with a chosen quick‑fix.
  - Constants for sane timeouts & executors.

- **`CompositeSuggestionEngine.java`**  
  Combines multiple engines. The provided implementation is **resilient**:
  if one child throws `SuggestionException`, it is swallowed and the composite
  still returns whatever the other engines produced. Issues are concatenated in
  input order; provider names remain on each `SuggestionIssue`.

- **`NoopSuggestionEngine.java`**  
  A `SuggestionEngine` that always returns an empty result. Good for smoke tests,
  offline mode, or to disable suggestions without null checks.

- **`SuggestionRequest.java`**  
  Immutable request describing *what* to analyze and *how*:
  - `text` (UTF‑16 indices apply), `language` (`en-US` default),
  - `scope` (`FULL_DOC`, `SENTENCE`, `WINDOW`) – engines may ignore,
  - `caret` (for incremental checks; `-1` if unknown),
  - `enabledCategories`, `disabledRuleIds`, `userDictionary` (all `Set<String>`).
  Includes convenience factory `of(String text)`.

- **`SuggestionIssue.java`**  
  One finding (spelling/grammar/style/punctuation). Fields:
  - `id` (UUID for UI), `ruleId`, `provider`, `type`, `severity`,
  - `start`, `end` (inclusive start, exclusive end), `message`,
  - `replacements` (0..N quick fixes), `meta` (arbitrary extras).  
  Includes `dedupKey()` to merge duplicates across engines.

- **`SuggestionResult.java`**  
  Wrapper for returned issues plus light diagnostics:
  - `issues` (immutable list), `elapsedMs`, `providerVersion`.
  Static helper: `empty()`.

- **`SuggestionException.java`**  
  Checked exception used by engines to surface provider failures (network,
  auth, bad input). The composite chooses whether to propagate or swallow.

- **Tests** (examples under `src/test/java/.../suggest/`)  
  - `NoopSuggestionEngineTest` – basic “returns empty” smoke test.  
  - `SuggestionEngineContractTest` – contract tests usable against any engine
    (fake engines inside the test).  
  - `CompositeSuggestionEngineTest` – verifies resilient fan‑out behavior and
    merging semantics.  

---

## Design patterns used

- **Strategy** – `SuggestionEngine` decouples *what the UI asks for* from *how
  suggestions are produced*.
- **Composite** – `CompositeSuggestionEngine` aggregates many strategies.
- **Null Object** – `NoopSuggestionEngine` avoids `null` checks in the app.
- **Value Object / Immutable DTOs** – `SuggestionRequest`, `SuggestionIssue`,
  `SuggestionResult` are immutable and safely shareable across threads.
- **Adapter (extension point)** – When you integrate a third‑party system
  (e.g., LanguageTool), write a `LanguageToolEngine` that **adapts** LT’s
  response into our `SuggestionIssue` model. This is the Adapter pattern.

> Not used: Builder. Constructors are kept short; if you prefer a builder for
> `SuggestionRequest`, it can be added without changing the interface.

---

## Thread‑safety & performance

- Value objects are immutable; safe to pass across threads.
- Engines should be stateless or guard any shared mutable state.
- `checkAsync` enables background evaluation to keep typing smooth.
- Offsets are **UTF‑16 code unit** indices (Java `String` semantics).

---

## Error handling policy

- Engines signal failure with `SuggestionException`.
- **Composite policy (current):** failures of individual children are swallowed;
  the composite still returns a result (possibly empty). This keeps UX stable
  even if one provider is flaky.
- If you prefer strict propagation, swap the composite implementation or add a
  flag to choose policy.

---

## Merging semantics

- Child results are concatenated; `provider` is preserved on each issue.
- Use `SuggestionIssue#dedupKey()` to de‑dup identical rule@span collisions
  across providers if needed.
- UI can group issues by `type`, `severity`, or by `provider`.

---

## Usage examples

### Synchronous
```java
SuggestionEngine engine = new CompositeSuggestionEngine(
        List.of(new NoopSuggestionEngine() /*, new LanguageToolEngine(...), ... */));

SuggestionResult r = engine.check(SuggestionRequest.of("Fix teh quick brun fox."));
r.issues.forEach(iss -> System.out.println(iss.message));
```

### Async (UI thread friendly)
```java
engine.checkAsync(SuggestionRequest.of("Fix teh quick brun fox."))
      .thenAccept(result -> {
          // update UI with result.issues
      });
```

### Applying a quick‑fix
```java
SuggestionIssue iss = r.issues.get(0);
String patched = SuggestionEngine.applyReplacement(originalText, iss);
```

---

## Adding a new engine (Adapter pattern)

1. Create `LanguageToolEngine implements SuggestionEngine`.
2. In `check(req)`, call LT (or your provider), map each finding to a
   `SuggestionIssue` (set `provider="LanguageTool"`), and return a
   `SuggestionResult` with `elapsedMs` and a version string.
3. Plug it into the composite in your DI or wiring code.

---

## Testing tips

- Use the contract tests as a template for new engines.
- Add golden tests for tricky spans and hyphenation.
- For network engines, provide a “fake” or “record/replay” mode to keep tests hermetic.

---

## FAQ

**Why inclusive start / exclusive end?**  
Matches Java substring semantics (`text.substring(start, end)`) and avoids off‑by‑one confusion.

**Why a checked exception?**  
To force call‑sites (or the composite) to make an explicit decision about failures.

**What about overlapping issues?**  
Allowed. The UI can stack, prioritize by severity, or de‑dup using `dedupKey()`.

---

## Versioning and compatibility

The API is small and stable. If you add fields, prefer additive changes to the
value objects and default values to keep binary compatibility.
