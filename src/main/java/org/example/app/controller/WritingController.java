package org.example.app.controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Tab;
import org.example.app.model.WritingConfig;
import org.example.app.model.WritingModel;
import org.example.app.preset.Preset;
import org.example.app.preset.PresetCapabilities;
import org.example.app.preset.PresetRegistry;
import org.example.app.view.WritingView;

/**
 * Controller orchestrates:
 *  - reacts to tab changes,
 *  - applies preset defaults,
 *  - reads current UI selections from the view,
 *  - builds WritingConfig and calls the model.
 */
public class WritingController {

    private final WritingModel model;
    private final WritingView view;

    public WritingController(WritingModel model, WritingView view) {
        this.model = model;
        this.view = view;
        wire();
        // Initialize to the first preset
        applyPresetDefaults(view.currentPresetKey());
    }

    private void wire() {
        // Generate button
        view.btnGenerate.setOnAction(e -> onGenerate());

        // Reset to preset defaults (for current tab)
        view.btnResetPreset.setOnAction(e -> {
            view.resetActivePresetInstructionToDefault();
            applyPresetDefaults(view.currentPresetKey());
        });

        // Tab change -> rebuild right column (view) + apply defaults
        ChangeListener<Tab> tabListener = (obs, oldTab, newTab) -> {
            String key = view.currentPresetKey();
            view.onPresetChanged(key);      // view composes sections for this preset
            applyPresetDefaults(key);       // set defaults into the sections
        };
        view.presetTabs.getTabs().getSelectionModel().selectedItemProperty().addListener(tabListener);
    }

    /** Sets section values from the preset's defaults. */
    private void applyPresetDefaults(String presetKey) {
        var all = PresetRegistry.all();
        Preset preset = all.getOrDefault(presetKey, all.values().iterator().next());

        var d = preset.defaults();
        // Temperature
        view.setTemperature(d.temperature());
        // Tone
        switch (d.tone()) {
            case INFORMAL -> view.setTone("informal");
            case FORMAL   -> view.setTone("formal");
            default       -> view.setTone("neutral");
        }
        // Style
        view.setStyle(d.grammarStyle());
        // Text mode
        switch (d.textMode()) {
            case SUMMARIZE -> view.setTextMode("summarize");
            case EXPAND    -> view.setTextMode("expand");
            default        -> view.setTextMode("same");
        }
        // Language: normalize to English when a preset does not expose translation
        if (!preset.capabilities().showTranslation()) {
            view.setLanguage("English");
        }
    }

    /** Reads UI → builds config → calls model → updates output. */
    private void onGenerate() {
        String presetKey = view.currentPresetKey();
        Preset preset = PresetRegistry.all().getOrDefault(
                presetKey, PresetRegistry.all().values().iterator().next()
        );
        PresetCapabilities caps = preset.capabilities();

        // Read current UI selections from the decoupled view
        double temperature = view.getTemperature();

        WritingConfig.Tone tone = switch (safe(view.getToneKey())) {
            case "informal" -> WritingConfig.Tone.INFORMAL;
            case "formal"   -> WritingConfig.Tone.FORMAL;
            default         -> WritingConfig.Tone.NEUTRAL;
        };

        // Capabilities drive overrides: if a section is hidden, force sensible values
        String grammarStyle = caps.showStyle() ? safe(view.getStyleKey()) : "none";

        WritingConfig.TextMode textMode =
                caps.showTextMode()
                        ? switch (safe(view.getTextModeKey())) {
                    case "summarize" -> WritingConfig.TextMode.SUMMARIZE;
                    case "expand"    -> WritingConfig.TextMode.EXPAND;
                    default          -> WritingConfig.TextMode.SAME;
                }
                        : WritingConfig.TextMode.SUMMARIZE; // hidden (e.g., Code Docs) → summarize behavior

        // Build config (model id and max tokens are arbitrary defaults you can expose later if you want)
        WritingConfig cfg = new WritingConfig(
                "gpt-4o-mini",
                temperature,
                800,
                tone,
                grammarStyle,
                textMode
        );
        model.applyConfig(cfg);

        // Compose final prompt:
        //   [optional translation instruction] + [preset instruction] + [user text]
        String language = caps.showTranslation() ? safe(view.getLanguage()) : "English";
        String transInstruction = (!"English".equalsIgnoreCase(language))
                ? "Write the final output in " + language + "."
                : "";

        String instruction = presetInstructionOrEmpty();
        String userText = safe(view.input.getText());

        String header = combineNonBlank(transInstruction, instruction);
        String finalPrompt = header.isBlank() ? userText : header + "\n\n---\n\n" + userText;

        // Call model
        view.btnGenerate.setDisable(true);
        view.status.setText("Requesting…");
        view.output.clear();

        model.generateAsync(finalPrompt).thenAccept(text ->
                Platform.runLater(() -> {
                    view.output.setText(text);
                    view.status.setText("Done");
                    view.btnGenerate.setDisable(false);
                })
        ).exceptionally(ex -> {
            Platform.runLater(() -> {
                view.output.setText("[Error] " + ex.getMessage());
                view.status.setText("Error");
                view.btnGenerate.setDisable(false);
            });
            return null;
        });
    }

    // ——— helpers ———

    private String presetInstructionOrEmpty() {
        String s = safe(view.currentPresetInstruction()).trim();
        return s;
    }

    private static String safe(String s) { return s == null ? "" : s; }

    /** Combines two strings with a newline if both are non-blank. */
    private static String combineNonBlank(String a, String b) {
        boolean A = a != null && !a.isBlank();
        boolean B = b != null && !b.isBlank();
        if (A && B) return a + "\n" + b;
        if (A) return a;
        if (B) return b;
        return "";
    }
}