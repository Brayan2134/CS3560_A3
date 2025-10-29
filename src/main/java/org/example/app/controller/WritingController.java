package org.example.app.controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Tab;
import org.example.app.model.WritingConfig;
import org.example.app.model.WritingModel;
import org.example.app.preset.Presets;
import org.example.app.view.WritingView;

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
        view.btnGenerate.setOnAction(e -> {
            String presetKey = view.currentPresetKey();

            // Determine grammarStyle and textMode based on preset visibility/override rules
            String grammarStyle = switch (presetKey) {
                case Presets.CREATIVE, Presets.CODEDOC -> "none"; // ignore style for these presets
                default -> view.style.getValue();
            };
            WritingConfig.TextMode mode = switch (presetKey) {
                case Presets.CODEDOC -> WritingConfig.TextMode.SUMMARIZE; // lock to summarize
                default -> switch (view.textMode.getValue()) {
                    case "summarize" -> WritingConfig.TextMode.SUMMARIZE;
                    case "expand"    -> WritingConfig.TextMode.EXPAND;
                    default          -> WritingConfig.TextMode.SAME;
                };
            };

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
            String langInstruction = (language != null && !language.equalsIgnoreCase("English"))
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

        // Tab change → defaults + visibility
        ChangeListener<Tab> tabListener = (obs, oldTab, newTab) -> {
            String key = view.currentPresetKey();
            applyPresetDefaultsAndVisibility(key);
        };
        view.tabs.getSelectionModel().selectedItemProperty().addListener(tabListener);
    }

    private void applyPresetDefaultsAndVisibility(String key) {
        // Defaults (temp/tone/style/text-mode)
        var p = Presets.all().getOrDefault(key, Presets.all().get(Presets.GENERAL));
        var c = p.defaults();

        view.temperature.setValue(c.temperature());
        switch (c.tone()) {
            case INFORMAL -> view.rbInformal.setSelected(true);
            case FORMAL -> view.rbFormal.setSelected(true);
            default -> view.rbNeutral.setSelected(true);
        }
        if (!view.style.getItems().contains(c.grammarStyle())) {
            view.style.getItems().add(c.grammarStyle());
        }
        view.style.getSelectionModel().select(c.grammarStyle());
        switch (c.textMode()) {
            case SUMMARIZE -> view.textMode.getSelectionModel().select("summarize");
            case EXPAND -> view.textMode.getSelectionModel().select("expand");
            default -> view.textMode.getSelectionModel().select("same");
        }

        // Visibility per preset
        switch (key) {
            case Presets.CREATIVE -> {
                view.setStyleVisible(false);     // hide style
                view.setTextModeVisible(true);   // keep text mode
                view.setTranslationVisible(true);
            }
            case Presets.CODEDOC -> {
                view.setStyleVisible(false);     // hide style
                view.setTextModeVisible(false);  // hide text mode
                view.setTranslationVisible(false); // hide translation for code docs
                // Reset to English to avoid stale hidden state
                view.translateLang.getSelectionModel().select("English");
            }
            default -> {
                view.setStyleVisible(true);
                view.setTextModeVisible(true);
                view.setTranslationVisible(true); // general + professional + academic
            }
        }
    }

    private String selectedTone() {
        if (view.rbInformal.isSelected()) return "informal";
        if (view.rbFormal.isSelected()) return "formal";
        return "neutral";
    }
}