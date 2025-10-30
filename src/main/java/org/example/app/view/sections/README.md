# `view/sections` — UI Composition & Patterns

This package contains **small, reusable UI blocks** that the main `WritingView` composes
into the right-hand control panel (temperature, tone, style, text-mode, translate, and
the preset tabs). The goal is to **decouple** the big view into well‑scoped components
that are easy to test, swap, or extend.

---

## Why this exists

- **Separation of concerns**: Each control lives in its own class (single responsibility).
- **No giant view classes**: `WritingView` is a *composer* only.
- **Pluggable UI**: The controller/view decide what to include based on preset *capabilities*.
- **Testability**: Each section can be instantiated and tested in isolation.
- **Predictable layout**: Sections expose a `Node` and common show/hide semantics.

---

## Key Interfaces & Classes

### `SectionView`
Minimal contract for any right‑pane section.

- `Node getNode()` — the root to add to layouts.
- `setVisible(boolean)` — default impl updates `managed` and `visible` together.

This lets `WritingView` treat all sections uniformly.

### `SectionEvents`
Observer callbacks emitted by sections when the user changes values:

- `onTemperatureChanged(double)`
- `onToneChanged(String)` — `"informal" | "neutral" | "formal"`
- `onStyleChanged(String)` — `"none" | "APA" | "MLA" | "Chicago"`
- `onTextModeChanged(String)` — `"summarize" | "same" | "expand"`
- `onLanguageChanged(String)` — e.g., `"English" | "Spanish" | ...`

`WritingView` implements this interface. The controller can ignore these and simply read
values via the view’s getters when generating.

### Concrete Sections
- `TemperatureSection` — slider with labeled axis and tooltip; getters/setters.
- `ToneSection` — radio group (Informal/Neutral/Formal).
- `StyleSection` — style combo (none/APA/MLA/Chicago).
- `TextModeSection` — summarize/same/expand combo.
- `TranslateSection` — language combo.
- `PresetTabsView` — the tabs across the top of the right pane with editable per‑preset instructions.

All sections are **self‑contained** and expose a small API for `WritingView` to set/read values programmatically.

### `SectionFactory`
Factory that returns a array of sections based on **capability flags** (from the selected preset).
This avoids `if/else` logic scattered through the view.

---

## Patterns Used

- **MVP-style Composition**: `WritingView` composes sections and forwards events.
- **Factory**: `SectionFactory` creates the appropriate set of sections for the active preset.
- **Observer**: `SectionEvents` propagates changes upward without tight coupling.
- **Interface-driven Design**: `SectionView` unifies how sections are added/hidden.

This is intentionally lightweight and grading-friendly (no external frameworks).

---

## Lifecycle in `WritingView`

1. On tab change, `WritingView` calls `rebuildRightColumnForPreset(key)`.
2. The method **clears the right VBox** (prevents duplicate-child errors), re-adds header + `PresetTabsView`,
   then asks `SectionFactory` for the correct sections.
3. It stores typed references (temperature/tone/… sections) for simple getters/setters.
4. Footer (Generate button + status) is re-added at the end.

---

## Adding a New Section

1. Create a class implementing `SectionView`.
2. (Optional) Accept a `SectionEvents` listener if the section should notify on changes.
3. Provide light getters/setters to read/write values programmatically.
4. Register it in `SectionFactory` (conditionally based on capabilities).
5. Use the new section’s getters/setters from `WritingView` as needed.

**Skeleton**
```java
public final class MyNewSection implements SectionView {
  private final TitledPane root = new TitledPane();
  private final SectionEvents events;

  public MyNewSection(SectionEvents events) {
    this.events = events;
    // build UI, add listeners, call events.on... when needed
  }

  @Override public Node getNode() { return root; }

  public void setValue(String v) { /* ... */ }
  public String getValue() { return /* ... */; }
}
```

---

## Testing Tips

- Instantiate each section and verify defaults (e.g., temperature slider starts at 0.5).
- Simulate a UI change and assert the section’s getter reflects the new state.
- Verify `WritingView.rebuildRightColumnForPreset(...)` clears and repopulates the right pane
  without throwing (no duplicate children).
- With a fake `SectionEvents`, assert callbacks fire when users interact with the controls.

---

## Summary

`view/sections` turns a large UI into small building blocks, composed by `WritingView`
based on preset capabilities. This improves readability, testability, and future extensibility
without introducing heavy frameworks.
