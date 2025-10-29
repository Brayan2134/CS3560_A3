
# Model Layer — Writing Assistant

This folder contains the **Model** portion of the MVC application that wraps the `OpenAIChatbotSDK`.  
It centralizes **state**, **configuration**, and **business logic** so the View and Controller never touch HTTP or API details.

> Files: `WritingConfig.java`, `WritingModel.java`, `WritingModelListener.java`

---

## Why this layer matters

- **Decoupling**: The UI (JavaFX) does not know about API keys, HTTP, or request formats. It only talks to the Model.
- **Testability**: The Model can be unit–tested with a fake SDK (no network). See `WritingModelTest`.
- **Single Source of Truth**: All generation parameters (temperature, style, tone, etc.) live in the Model’s config.
- **Extensibility**: You can layer Strategy/Factory/Observer patterns above without changing the UI.

---

## Files Overview

### 1) `WritingConfig.java` (Immutable configuration)
Represents a **snapshot of knobs** for a single generation request.

- **Fields**: `model`, `temperature`, `maxOutputTokens`, `tone`, `grammarStyle`, `textMode`
- **Enums**:
  - `Tone` = `INFORMAL | NEUTRAL | FORMAL`
  - `TextMode` = `SUMMARIZE | SAME | EXPAND`
- **Validation**: Ensures `temperature ∈ [0,2]` and `maxOutputTokens > 0`.
- **Helpers**:
  - `expand()` and `summarize()` expose boolean toggles the SDK expects.
  - `defaults()` gives a safe baseline for the entire app.

**Why**: Gives the Controller a single, simple object to build from UI controls. Keeps Model code clean.

---

### 2) `WritingModel.java` (Business logic + SDK wrapper)
Owns a single instance of `OpenAIChatbotSDK`, applies the active `WritingConfig`, and exposes an **async** generation method.

- **State**: `activeConfig` (starts at `WritingConfig.defaults()`).
- **API**:
  - `applyConfig(WritingConfig cfg)` — maps your config to SDK knobs
  - `generateAsync(String userText)` — returns `CompletableFuture<String>`
- **Mapping** (UI → SDK):
  - Tone: `INFORMAL → "casual"`, `NEUTRAL → "neutral"`, `FORMAL → "formal"`
  - Style: `"none" → "standard"` (else pass-through)
  - TextMode: toggles `setTextExpansion`/`setTextSummarization`

**Why**: Centralizes all side-effects (HTTP calls) behind one clean async API so the Controller can show a spinner and remain responsive.

---

### 3) `WritingModelListener.java` (Observer hook)
A very small listener interface so the Controller/View can react to model state changes:

- `onRequestStart(prompt)`
- `onRequestComplete(output)`
- `onRequestError(code, message)`

**Why**: Satisfies the Observer pattern requirement and keeps UI updates event-driven.

---

## Lifecycle (typical user flow)

```
User types text → Controller builds WritingConfig from UI → Model.applyConfig(cfg)
→ Controller calls Model.generateAsync(text)
→ Model asks SDK (POST /responses) and then fetches result (GET /responses/{id})
→ Model notifies listeners: start/complete/error → Controller updates View
```

---

## Example Usage (Controller-side)

```java
WritingConfig cfg = new WritingConfig(
      "gpt-4o-mini", 0.6, 600,
      WritingConfig.Tone.FORMAL, "APA",
      WritingConfig.TextMode.SUMMARIZE
);
  
  model.applyConfig(cfg);
  model.generateAsync(userText)
       .thenAccept(result -> Platform.runLater(() -> view.output.setText(result)));
```

---

## Testing Notes

- **`WritingModelTest`** injects a **Fake SDK** that overrides `askQuery/getQuery` and records knob values.
  - Verifies config → SDK mapping (tone/style/toggles).
  - Confirms async behavior & listener callbacks (COMPLETE / ERROR / PENDING).
- **`WritingConfigTest`** checks defaults, normalization, and range validation.

This allows grading your architecture without needing a live API key.

---

## Extension Points

- **Strategy Pattern**: Create `AIStrategy` implementations that produce different `WritingConfig` presets (Creative, Professional, Academic). The Controller switches strategies, then calls `model.applyConfig(...)`.
- **Factory Pattern**: If you add multiple request types later (e.g., Summarize vs. Rewrite vs. Translate), implement a `RequestFactory` that returns different `Runnable`/`Callable` tasks which the Controller submits.
- **Streaming**: Replace the current `generateAsync` internals with a streaming loop later; listeners will still work for incremental updates.

---

## Mini MVC Diagram (ASCII)

```
+---------+          +--------------+          +-------------------+
|  View   |  events  |  Controller  |  config  |   WritingModel    |
| (JavaFX)| ───────▶ | (Glue code)  | ───────▶ | (SDK + business)  |
|         | ◀─────── |              | results  |                   |
+---------+  updates +--------------+          +-------------------+
                                               │ OpenAIChatbotSDK │
                                               +-------------------+
```

---

## Gotchas / Tips

- Ensure `OPENAI_API_KEY` is visible to the process (`./gradlew run` is safest).
- Keep View dumb: no API keys, no HTTP, no threads.
- Use `WritingConfig.defaults()` to avoid null/blank values.
- Long operations should always run via `generateAsync` to prevent UI freeze.