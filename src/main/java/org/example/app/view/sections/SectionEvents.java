package org.example.app.view.sections;

/** Observer callbacks from sections up to the parent (optional to use). */
public interface SectionEvents {
    default void onTemperatureChanged(double value) {}
    default void onToneChanged(String toneKey) {}
    default void onStyleChanged(String styleKey) {}
    default void onTextModeChanged(String modeKey) {}
    default void onLanguageChanged(String language) {}
}