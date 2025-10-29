package org.example.app.preset;

/** Declarative UI toggles so the View/Controller don't switch on preset keys. */
public record PresetCapabilities(
        boolean showStyle,
        boolean showTextMode,
        boolean showTranslation
) {
    public static PresetCapabilities allVisible() {
        return new PresetCapabilities(true, true, true);
    }
    public static PresetCapabilities hideStyle() {
        return new PresetCapabilities(false, true, true);
    }
    public static PresetCapabilities codeDocs() {
        return new PresetCapabilities(false, false, false);
    }
}