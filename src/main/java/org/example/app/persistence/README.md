# CS3560_A3 – Intelligent Writing Assistant (Architecture & Persistence)

This README explains **what the core modules are**, **how they fit together**, and **why they exist** in the application. It also highlights the **design patterns** in use (or natural fits) so you can cite them in your report.

---

## High‑Level Architecture (MVC + Services)

The app follows a classic **MVC** split with a thin service layer:

- **Model (`app.model`)**
  - Holds the in‑memory writing state (instructions, input/output text, config).
  - Notifies listeners when state changes (see _Observer Pattern_ below).

- **View (`app.view`)**
  - Swing UI (`WritingView`, `sections/*`) that renders the model and exposes controls (text areas, buttons, menu items, etc.).
  - No persistence or network logic lives here—just UI.

- **Controller (`app.controller.WritingController`)**
  - Mediates between View and Model.
  - Orchestrates persistence (`SessionManager`, `LastSessionStore`) and exports (`DocumentExporter`).
  - Triggers API calls via the OpenAI SDK package (`openaichatbotsdk`, if configured).

- **Persistence (`app.persistence`)**
  - Filesystem‑based session store: creates sessions, saves immutable revisions, loads/reverts, exports.
  - Keeps a JSON **manifest** per session with lightweight metadata so the UI can browse without loading heavy content.
  - (See detailed docs below.)

- **Presets (`app.preset`)**
  - A family of “modes” (e.g., Academic, Creative, Professional) that shape model behavior and UI defaults.
  - Implements a **Strategy** (the preset interface) and a **Factory/Registry** (to resolve preset keys).

- **Utilities (`app.util`)**
  - Small helpers that encapsulate reusable logic (e.g., statistics, syllable counts, lightweight suggestions).

- **OpenAI SDK (`openaichatbotsdk`)**
  - Integration point for model calls. The controller hands it instructions and input; the SDK returns output.

The net effect: **controllers orchestrate**, **models store state**, **views render**, **persistence saves**, and **presets configure behavior**.

---

## Persistence Module (why each class exists)

All classes live in `org.example.app.persistence`.

### `SessionManager`
**What it is.** The central, file‑based repository for sessions and their revisions.  
**Why it exists.** To decouple persistence from UI/logic and keep save/load/revert behavior consistent and testable.  
**How it works.**
- Layout:
  ```text
  <root>/<sessionId>/manifest.json
  <root>/<sessionId>/revisions/<revisionId>.json
  ```
- `createSession(title)` creates folders and an empty manifest.
- `saveRevision(sessionId, snapshot, note)` writes a revision file and appends to the manifest (oldest→newest).
- `listSessions()` reads headers from each manifest and sorts **newest‑first** using `updatedAt` (fallback `createdAt`).
- `listRevisions(sessionId)` returns **oldest→newest** so “latest” is the last element.
- `loadRevision(sessionId, revisionId)` / `loadLatest(sessionId)` read JSON files and deserialize to `SessionSnapshot`.
- `revertToRevision(sessionId, revisionId)` deletes later revision files and trims the manifest.
- `deleteSession(sessionId)` recursively removes the whole directory.

**Design pattern references.**
- _Repository pattern_ (a.k.a. DAO): central point for session/revision I/O.
- _Adapter_ (minor): custom Gson adapter for `Instant` so timestamps round‑trip as ISO‑8601.

---

### `SessionSnapshot`
**What it is.** Immutable payload of a single “writing state”: instructions, input, output, model id, temperature, max tokens, tone, style, language, and `capturedAt`.  
**Why it exists.** A revision needs the **full context** to reproduce or export results later.  
**How it works.** Pure data holder, easy to serialize with Gson; exporters and the UI consume it directly.

---

### `SessionInfo`
**What it is.** Lightweight session metadata (id, title, created/updated times).  
**Why it exists.** The UI can list/sort sessions using **only** this—no need to load snapshots.  
**How it works.** Stored in the session’s manifest; `SessionManager.listSessions()` reads these headers quickly.

---

### `RevisionInfo`
**What it is.** Lightweight revision metadata (id, `createdAt`, optional `note`).  
**Why it exists.** The UI can render a revision list fast, without loading large snapshot bodies.  
**How it works.** Stored as an array in `manifest.json` (chronological, oldest→newest).

---

### `LastSessionStore`
**What it is.** Tiny key‑value store that persists the **last opened session id** in a small JSON file.  
**Why it exists.** On launch, the app can immediately reopen the previous session without scanning directories.  
**How it works.** `write(id)` and `read()` with graceful handling of missing/corrupt files.

---

### `DocumentExporter`
**What it is.** Stateless utility for exporting a `SessionSnapshot` to TXT/MD (and PDF if your build includes iText).  
**Why it exists.** Keeps a single, testable place for all export formats—controllers don’t duplicate templates.  
**How it works.**
- TXT uses `=== Instruction === / === Input === / === Output ===` sections and a config block.
- MD uses `# / ##` headings and back‑tick metadata values.
- PDF (optional) uses standard fonts and code‑like formatting for Input/Output.
- All exporters are deterministic and UTF‑8.

**Design pattern references.**
- _Strategy (format)_ – the class embodies multiple output “strategies” (TXT/MD/PDF) behind separate methods.
- _Single‑responsibility_ – formatting is kept away from controllers/models.

---

## Preset Module

Located in `org.example.app.preset`.

- **`Preset` (interface/abstract) & concrete presets** (`AcademicPreset`, `CreativePreset`, `ProfessionalPreset`, `GeneralPreset`, etc.)  
  Each preset defines capability flags and sensible defaults (tone, style, temperature caps, etc.).

- **`PresetRegistry`**  
  Acts as a **Factory/Registry**: given a `presetKey` it returns the corresponding preset implementation. This is the cleanest way to map strings from the model/config to concrete strategies without `if/else` chains scattered in the UI.

- **`PresetCapabilities`**  
  Encodes what a preset supports (e.g., translation, rewriting, coding aids). This keeps feature checks declarative.

**Design patterns here.**
- **Strategy Pattern** – _the_ core pattern: each preset is a strategy for “how to write.”
- **Factory/Registry Pattern** – resolving `presetKey → Preset` in one place (extensible without touching controllers).

> If you later allow “stacked” presets (e.g., base “General” + “Technical Jargon” overlay), you could introduce a **Decorator** that wraps a base preset and tweaks fields (temperature, tone, vocab list).

---

## Utilities

- **`Suggestion` / `SuggestionEngine`** – encapsulate low‑risk, local suggestions (e.g., small rephrasings) so the UI doesn’t do text math itself.
- **`TextStats` / `SyllableCounter`** – small, deterministic helpers for counts and readability statistics.

These are model‑agnostic and easily unit‑tested.

---

## Design Patterns Summary

- **MVC** – clear separation: View (Swing), Controller (event logic), Model (state).
- **Observer Pattern** – the model typically exposes a `WritingModelListener`; views subscribe and react to state changes.
- **Strategy Pattern** – presets implement a common contract and can be swapped by key at runtime.
- **Factory/Registry Pattern** – `PresetRegistry` resolves keys to concrete strategies.
- **Repository (DAO)** – `SessionManager` centralizes file I/O and manifest management.
- **Adapter (minor)** – Gson `Instant` adapter for stable time serialization.
- **(Optional) Decorator** – a natural extension if you add “overlay” presets.

---

## How It Flows (Typical Use)

1. **User edits** text in the Swing UI (`WritingView`).  
2. **Controller** updates the **Model** and may call the **OpenAI SDK** to generate output.  
3. The **Model** notifies the **View** (Observer).  
4. When the user saves, the **Controller** calls **`SessionManager.saveRevision`** with a `SessionSnapshot`.  
5. On startup, **`LastSessionStore`** returns the last session id so the **Controller** reopens it and displays revisions.  
6. When exporting, **`DocumentExporter`** formats the most recent snapshot into TXT/MD/PDF.

---

## Error Handling & Async

- **Async UI**: Long‑running tasks (API calls, large exports) should run off the EDT (e.g., `SwingWorker`) to keep the UI responsive.
- **Persistence errors**: Constructors avoid checked exceptions (e.g., `SessionManager(Path)`); runtime failures surface as `IOException` at call sites where the controller can show a toast/dialog and keep the app alive.
- **Grace**: `LastSessionStore.read()` returns `null` on missing/corrupt files—start cleanly.

---

## Testing Notes

Recommended unit tests (some are already provided):
- `SessionManager` end‑to‑end flow: create → save two revisions → list → loadLatest → revert → delete.
- `DocumentExporter` content checks (TXT & MD exact markers).
- `LastSessionStore` round‑trip and corrupt‑file handling.
- `PresetRegistry` resolution from keys; capabilities toggles.
- `SuggestionEngine` determinism.

---

## Why this structure works

- **Replaceable**: presets and exporters are swappable without touching unrelated modules.
- **Fast UI**: metadata manifests allow instant lists; snapshots load lazily.
- **Safe**: revisions are immutable files; revert is simple and deterministic.
- **Testable**: persistence, exporters, and utilities are standalone and easy to cover with JUnit.

---

If you want a diagram (class or sequence), ping me and I’ll add one.
