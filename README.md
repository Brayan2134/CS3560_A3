# Intelligent Writing Assistant

A JavaFX application that allows user's to interface with OpenAI for accomplishing writing tasks. This can be used
for general, academic, professional, and creative settings, allowing users to dynamic adjust settings to be tailored
to their specific use-case.

## Requirements
- JDK 17+
- Gradle 8+ (wrapper included)
- *Internet* (for API calls)

## Setup
1. Get `OPENAI_API_KEY` from openai.com
2. Save as env variable

## Run
- `./gradlew run`
- `run org.example.app.MainApp`

## Build/test
- `./gradlew build`
- `./gradlew test`

# Features
- **Presets**: General, Creative, Professional (editable prompt text)
  - Using `gpt-4o-mini`
- **Live stats**: Words, sentences, FRE (Flesch Reading Ease), FKGL (Flesch–Kincaid Grade Level)
- **Suggestion system**: pluggable engines, non-blocking, fault-tolerant
  - Using `LanguageTool` engine
- **Async UI**: debounced checks, background futures (no UI freezes)
- **Save/Load ready**: model, view, controller separated (persistence hooks in place)
- **_BONUS: multi-language translate section_**

# Design Patterns
- Strategy: `suggest/SuggestionEngine`
  - Where: `SuggestionEngine` is the common interface all engines implement
  - Why: Easy swapping of providers (you can see `FakeSpellEngine` for testing and `LanguageToolSuggestionEngine`) for real implementation
- Composite: `suggest/CompositeSuggestionEngine`
  - Where: Fans out one request to many engines and merges results
  - Why: Combine spelling/grammar/style engines and stay resilient if one engine fails (others still return issues)
- Null Object: `suggest/NoopSuggestionEngine`
  - Where: A do-nothing engine that always returns an empty result
  - Why: Useful for wiring the UI, offline mode, or tests without littering the app with null/feature flags
- Adapter: `LanguageToolSuggestionEngine`
  - Where: Adapts _LangaugeTool_ API Into `SuggestionIssue` and `SuggestionResult`
- Observer (UI Callbacks): `view/sections/SectionEvents`
  - Where:Sections notify the host view via `onTemperatureChanged`, `onToneChanged`, etc
  - Why: Decouples each section from the rest of the UI; the view/controller can react without tight coupling

# YouTube Video
[Video Link](https://youtu.be/5VYqxMmWgww)