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

/** Glue: reads the view, builds config, applies preset defaults, calls model. */
public class WritingController {

    private final WritingModel model;
    private final WritingView view;

    public WritingController(WritingModel model, WritingView view) {
        this.model = model;
        this.view = view;
        wire();
        applyPresetDefaultsAndVisibility(view.currentPresetKey());
    }

    private void wire() {
        // Generate
        view.btnGenerate.setOnAction(e -> {
            String presetKey = view.currentPresetKey();
            Preset preset = PresetRegistry.all().get(presetKey);
            if (preset == null) {
                // fallback to first preset if somehow missing
                preset = PresetRegistry.all().values().iterator().next();
            }
            PresetCapabilities caps = preset.capabilities();

            // Derive grammarStyle + text mode from capabilities (not from keys)
            String grammarStyle = caps.showStyle() ? view.style.getValue() : "none";
            WritingConfig.TextMode mode =
                    caps.showTextMode()
                            ? switch (view.textMode.getValue()) {
                        case "summarize" -> WritingConfig.TextMode.SUMMARIZE;
                        case "expand"    -> WritingConfig.TextMode.EXPAND;
                        default          -> WritingConfig.TextMode.SAME;
                    }
                            : WritingConfig.TextMode.SUMMARIZE; // hide => treat as summarize (e.g., Code Docs)

            // Build config from UI (with the overrides above)
            WritingConfig cfg = new WritingConfig(
                    "gpt-4o-mini",
                    view.temperature.getValue(),
                    800,
                    switch (selectedTone()) {
                        case "informal" -> WritingConfig.Tone.INFORMAL;
                        case "formal"   -> WritingConfig.Tone.FORMAL;
                        default         -> WritingConfig.Tone.NEUTRAL;
                    },
                    grammarStyle,
                    mode
            );

            model.applyConfig(cfg);

            // Compose final prompt = (translation instruction?) + preset instruction + user text
            String language = view.translateLang.getValue();
            String langInstruction =
                    (caps.showTranslation() && language != null && !language.equalsIgnoreCase("English"))
                            ? "Write the final output in " + language + "."
                            : "";

            String instruction = view.currentPresetInstruction().trim();
            String userText = view.input.getText();

            String combinedInstruction = instruction.isBlank()
                    ? langInstruction
                    : (langInstruction.isBlank() ? instruction : (langInstruction + "\n" + instruction));

            String finalPrompt = combinedInstruction.isBlank()
                    ? userText
                    : combinedInstruction + "\n\n---\n\n" + userText;

            view.btnGenerate.setDisable(true);
            view.status.setText("Requesting…");
            view.output.clear();

            model.generateAsync(finalPrompt).thenAccept(resultText ->
                    Platform.runLater(() -> {
                        view.output.setText(resultText);
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
        });

        view.btnResetPreset.setOnAction(e -> view.resetActivePresetInstructionToDefault());

        // Tab change → defaults + visibility from capabilities
        ChangeListener<Tab> tabListener = (obs, oldTab, newTab) -> {
            String key = view.currentPresetKey();
            applyPresetDefaultsAndVisibility(key);
        };
        view.tabs.getSelectionModel().selectedItemProperty().addListener(tabListener);
    }

    private void applyPresetDefaultsAndVisibility(String key) {
        var all = PresetRegistry.all();
        Preset preset = all.getOrDefault(key, all.values().iterator().next());
        var c = preset.defaults();

        // Apply default knobs
        view.temperature.setValue(c.temperature());
        switch (c.tone()) {
            case INFORMAL -> view.rbInformal.setSelected(true);
            case FORMAL   -> view.rbFormal.setSelected(true);
            default       -> view.rbNeutral.setSelected(true);
        }
        if (!view.style.getItems().contains(c.grammarStyle())) {
            view.style.getItems().add(c.grammarStyle());
        }
        view.style.getSelectionModel().select(c.grammarStyle());
        switch (c.textMode()) {
            case SUMMARIZE -> view.textMode.getSelectionModel().select("summarize");
            case EXPAND    -> view.textMode.getSelectionModel().select("expand");
            default        -> view.textMode.getSelectionModel().select("same");
        }

        // Toggle UI via capabilities
        var caps = preset.capabilities();
        view.setStyleVisible(caps.showStyle());
        view.setTextModeVisible(caps.showTextMode());
        view.setTranslationVisible(caps.showTranslation());

        // Reset translation when hidden to avoid stale state
        if (!caps.showTranslation()) {
            view.translateLang.getSelectionModel().select("English");
        }
    }

    private String selectedTone() {
        if (view.rbInformal.isSelected()) return "informal";
        if (view.rbFormal.isSelected()) return "formal";
        return "neutral";
    }
}