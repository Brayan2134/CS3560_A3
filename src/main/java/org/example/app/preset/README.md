# Presets Module — Architecture & Design Rationale

This folder implements the **preset system** for the Intelligent Writing Assistant. Each preset (General, Creative, Professional, Academic, Code Docs) represents a cohesive *strategy* for default behavior and UI capabilities. The goal is to keep the UI free of `switch`/`if` logic tied to preset *names* and instead depend on preset *contracts* and *capabilities*.

---

## High-Level Goals

- **Decoupling & Modularity** — UI doesn’t need to know “how” a preset behaves; it only reads a preset’s defaults and capabilities.
- **Open/Closed Principle** — add or modify presets without changing controller/view code.
- **Single Responsibility** — each preset owns defaults and its default instruction; the controller aggregates them.
- **Testability** — presets are plain classes with no framework/state coupling; simple unit tests verify defaults/capabilities.
- **Grading-Friendly** — clear, idiomatic use of OOP patterns with minimal magic.

---

## Key Classes

### `Preset` (interface)
**Pattern:** *Strategy*  
Defines the contract every preset must implement:
- `key()` — stable identifier (e.g., `general`, `creative`).
- `title()` — tab label.
- `defaults()` — a `WritingConfig` with model, temperature, max tokens, tone, style, and text-mode.
- `defaultInstruction()` — editable seed prompt for that preset’s tab.
- `capabilities()` — UI toggles (see below).

### `PresetCapabilities` (record)
**Pattern:** *Data/Value Object*  
Declares which UI controls are applicable for a preset:
- `showStyle` — show/hide **Style** dropdown (APA/MLA/Chicago).
- `showTextMode` — show/hide **Text mode** (summarize/same/expand).
- `showTranslation` — show/hide **Translate** language picker.

> Example: Creative hides **Style**; Code Docs hides **Style**, **Text mode**, and **Translate**.

### Concrete Strategies
- `GeneralPreset`
- `CreativePreset`
- `ProfessionalPreset`
- `AcademicPreset`
- `CodeDocPreset`

Each class encapsulates sensible defaults and a short default instruction tailored to its use case.

### `PresetRegistry`
**Pattern:** *Registry / Provider*  
Returns a **LinkedHashMap** of all presets in a fixed order for tab creation. This prevents hard-coding in the UI and stabilizes tab order.

```java
Map<String, Preset> all() {
  // order matters for tab layout
  general, creative, professional, academic, codedoc
}
```

---

## How the MVC Uses This Module

- **View (`WritingView`)** builds preset tabs by iterating `PresetRegistry.all()` and populates each tab with its preset’s `defaultInstruction()`.
- **Controller (`WritingController`)**:
  - On tab switch, reads `defaults()` to apply temperature/tone/style/text-mode.
  - Reads `capabilities()` to **show/hide** Style, Text mode, and Translation sections.
  - When generating, it **derives overrides from capabilities** (e.g., if a section is hidden, style is treated as `"none"`; if text-mode hidden, it defaults to `SUMMARIZE`). No `switch` on preset keys.

This keeps the UI **data-driven** (capabilities & defaults) and eliminates preset-specific branching in the controller.

---

## Design Patterns Used

1. **Strategy** — `Preset` is the strategy. Each concrete preset is a strategy implementation with its own defaults and instruction.
2. **Registry** — `PresetRegistry` centralizes discovery/ordering, enabling dynamic tab building without `switch/case` in the UI.
3. **Value Object** — `PresetCapabilities` expresses UI applicability declaratively, avoiding conditionals scattered across view/controller.
4. **(Optional) Decorator** — Future extension: wrap an existing `Preset` to mutate `defaults()`, `defaultInstruction()`, or `capabilities()` at runtime (e.g., “Creative + Formal Tone”). Not required by the assignment but easy to add.

---

## Why This Architecture?

- **Open/Closed**: To add a new preset (e.g., “Technical Report”), create one class and register it—no controller/view edits required.
- **Separation of Concerns**: Presets own domain defaults; the controller orchestrates; the view renders.
- **Predictable UI**: Capabilities control visibility; the same capabilities also drive *behavioral overrides* at generation time.
- **Unit-Testable**: Presets and capabilities can be tested without the UI (e.g., temperature defaults, tone, visibility flags).

---

## Adding a New Preset (Step-by-Step)

1. **Create a class** implementing `Preset` (e.g., `TechnicalReportPreset`):
   - Return a unique `key()` and friendly `title()`.
   - Provide a `defaults()` `WritingConfig` (temperature, tone, etc.).
   - Write a succinct `defaultInstruction()`.
   - Choose `capabilities()` (which controls are relevant).
2. **Register it** in `PresetRegistry` (order = tab order).
3. **Write a small unit test** to verify its defaults/capabilities.

No controller/view changes are needed.

---

## Behavioral Rules (Controller)

- If `showStyle == false` ⇒ `grammarStyle = "none"` regardless of UI selection.
- If `showTextMode == false` ⇒ `textMode = SUMMARIZE` (for Code Docs-style presets).
- If `showTranslation == false` ⇒ reset dropdown to **English** and ignore translation instruction.
- Translation is appended as a short instruction (e.g., “Write the final output in Spanish.”) before the preset instruction.

---

## Error Handling & Edge Cases

- **Missing preset key**: Controller falls back to the first registry entry.
- **Stale UI state**: When a section is hidden, the controller normalizes the underlying config (e.g., style resets to `none`). The view also resets translation to English if the Translate section is hidden.
- **Layout**: Tab min-width and right-pane width are tuned so all five tabs are visible. If you add more, consider vertical tabs.

---

## Testing Recommendations

- **PresetRegistryTest** — Verify there are 5 presets in the expected order.
- **EachPresetTest** — Assert `defaults()` and `capabilities()` values (temperature, tone, visibility flags).
- **ControllerConfigTest** — Given a preset with `showStyle=false`, ensure the built config forces `style="none"`, etc.

Sample snippet:

```java
var map = PresetRegistry.all();
var creative = map.get("creative");
assertFalse(creative.capabilities().showStyle());
assertEquals(0.8, creative.defaults().temperature(), 1e-6);
```

---

## Future Extensions

- **Decorator Presets** (compose behavior at runtime).
- **Profile Persistence** (save per-preset knob overrides and reload them).
- **Telemetry** (log generation time, token usage by preset).
- **A/B Testing** (compare defaults across cohorts without changing code).

---

## Summary

This module keeps presets **pluggable**, UI **data-driven**, and code **maintainable**. It demonstrates Strategy + Registry + Value Object patterns to produce a scalable and testable design suitable for an OOP-focused assignment.
