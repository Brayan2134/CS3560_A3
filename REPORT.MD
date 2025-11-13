# Project Report

## Challenges I Faced
**Challenge 1: Right-side layout overflow & hidden suggestions box**

- Problem: Right column overflowed the window and (by design) the suggestions box was set `invisible/managed(false)` for earlier UI constraints.
- Solution: Clamp the right scroll pane width (e.g., 420px), disable horizontal scrollbars, and ensure suggestions’ visibility matches the UX design (on paper).
- Learned: Create a viewport_width variable that propagates throughout all views, forcing equal dimensions during rendering.

**Challenge 2: Suggestions  aren't showing**
- Problem: I typed in the input pane and nothing appeared in the suggestions list; fake spell check seemed to do nothing.
- Solution: Wire `inputTextProperty()` listener; implement `showSuggestions(List<SuggestionIssue>)`; ensure indexes are clamped to the input editor length; verify a minimal engine (FakeSpellCheck) returns issues.
- Learned: Don't finish half features before you log off and if you do, actually comment the work and future todo's. Seeing half-baked methods gave me the impression we were doing something else.

## Design Pattern Justifications
### Strategy
- **Usage**:
  - `org.example.app.suggest.SuggestionEngine` - Strategy interface
  - `NoopSuggestionEngine` - Concrete strategy
  - `LanguageToolSuggestionEngine` - Concrete strategy
- **Why**:
  - You need to swap between “providers” of suggestions (spell/grammar/style): a fake/local one during development, LanguageTool locally, or a remote HTTP service later—without touching UI or controller code
- **Benefits**:
  - Hot-swappable providers (dev uses `Noop`, prod uses `LanguageTool`)
  - Separate concerns (UI and control flow don’t know about provider quirks)
  - Testability (inject a deterministic test engine)
- **Tradeoffs**:
  - Slightly more boilerplate (interface + multiple implementations)
  - You must pass a strategy instance to clients (DI / wiring)

### Composite
- **Usage**:
  - `CompositeSuggestionEngine` aggregates multiple `SuggestionEngines` and exposes the same interface
- **Why**:
  - No one engine does everything. You want to layer spelling + grammar + style engines (and later an LLM) and present a single, merged result to the UI. This happens by:
    - Fanning out `check()`/`checkAsync()` to children
    - Fail-soft per engine (swallows child exceptions)
    - Merge and return one `SuggestionResult`
- **Benefits**:
  - Extensibility: Add/remove engines without touching callers
  - Uniformity: Callers treat “one engine” the same as “many engines”
- **Tradeoffs**:
  - Need a clear merge policy (de-dupe, ordering, severity precedence)
  - Latency can grow with the slowest child

### Observer
- **Usage**:
  - JavaFX property/listener model in `WritingView` and `WritingController`
    - `input.textProperty().addListener(...)`
    - `view.inputTextProperty().addListener(onInputChanged)`
    - Controller updates `view.showSuggestions(...)` on completion
- **Why**:
  - The UI should react to live input without polling or blocking. As the user types, you want to debounce and trigger async suggestion checks, then update the UI
- **Benefits**:
  - Responsive UX without tight coupling
  - Clear flow: input → (debounce) → async check → UI updates
- **Tradeoffs**:
  - Callbacks can become tangled if not well-structured; keep handlers small and focused
  - Be careful with threading (do UI updates on JavaFX thread)

### Builder
- **Usage**:
  - `SuggestionRequest.of(String text)` and the telescoping constructor in `SuggestionRequest`
- **Why**:
  - Requests have many optional knobs (language, scope, caret, enabled/disabled rule sets, dictionary). The static factory keeps the common path ergonomic while allowing advanced callers to specify everything
- **Benefits**:
  - Readable construction for common cases: `SuggestionRequest.of(text)`
  - Extensibility: Easy to add more fields later without breaking call sites
- **Tradeoffs**:
  - Not a full builder. I didn't add a complete implementation like:
    ```
    SuggestionRequest req = SuggestionRequest.builder()
      .text(text).language("en-US").scope(FULL_DOC).caret(caret)
      .enabledCategories(Set.of("grammar","spelling"))
      .userDictionary(userWords)
      .build();
    ```

### Adapter
- **Usage**:
  - `LanguageToolSuggestionEngine` wraps LanguageTools’ JLanguageTool API and adapts `RuleMatch` results → `SuggestionIssue` & `SuggestionResult`
- **Why**:
  - UI and controller are developed for the problem-statement (ex: `SuggestionIssue`, `SuggestionResult`, UTF-16 offsets, message/severity), while `LanguageTool` has its own severity/rule identifiers, and specific offset semantics. The adapter allows full interoperability between the API and app
- **Benefits**:
  - Isolation from third-party API changes
  - Keeps app provider-agnostic
- **Tradeoffs**:
  - Must write and maintain a small mapping layer (rule IDs, types, severities, offsets)

## AI Usage
I did use ChatGpt on this project! Mainly, it was used for documentation (file headers, method headers, and function comments).
All core methods were written by me, ChatGpt would help me optimize my code (like sectioning code or overloading when applicable).

## Time-spent
About 15 hours (over the course of 3 weeks)