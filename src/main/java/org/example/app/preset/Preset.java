package org.example.app.preset;

import org.example.app.model.WritingConfig;

/** A writing preset with defaults + an editable instruction template. */
public interface Preset {
    /** Unique key used in the UI (tab id). */
    String key();

    /** Tab display title. */
    String title();

    /** Default config knobs for this preset. */
    WritingConfig defaults();

    /** Default instruction text shown in the tab; user can edit it. */
    String defaultInstruction();
}