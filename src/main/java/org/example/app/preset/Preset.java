package org.example.app.preset;

import org.example.app.model.WritingConfig;

/** Strategy for a writing preset: defaults + instruction + UI capabilities. */
public interface Preset {
    /** Stable key (e.g., "general", "creative"). */
    String key();
    /** Tab title. */
    String title();
    /** Default knobs for this preset. */
    WritingConfig defaults();
    /** Default editable instruction shown in the tab. */
    String defaultInstruction();
    /** UI capabilities for toggling panels on/off. */
    PresetCapabilities capabilities();
}