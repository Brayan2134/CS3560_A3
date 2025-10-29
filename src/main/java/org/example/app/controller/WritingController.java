package org.example.app.controller;

import javafx.application.Platform;
import org.example.app.model.WritingConfig;
import org.example.app.model.WritingModel;
import org.example.app.view.WritingView;

/** Glue: reads the view, builds config, calls model, updates view. */
public class WritingController {

    private final WritingModel model;
    private final WritingView view;

    public WritingController(WritingModel model, WritingView view) {
        this.model = model;
        this.view = view;
        wire();
    }

    private void wire() {
        view.btnGenerate.setOnAction(e -> {
            // 1) Build config from UI
            WritingConfig cfg = new WritingConfig(
                    "gpt-4o-mini",
                    view.temperature.getValue(),
                    800,
                    switch (selectedTone()) {
                        case "informal" -> WritingConfig.Tone.INFORMAL;
                        case "formal"   -> WritingConfig.Tone.FORMAL;
                        default         -> WritingConfig.Tone.NEUTRAL;
                    },
                    view.style.getValue(),
                    switch (view.textMode.getValue()) {
                        case "summarize" -> WritingConfig.TextMode.SUMMARIZE;
                        case "expand"    -> WritingConfig.TextMode.EXPAND;
                        default          -> WritingConfig.TextMode.SAME;
                    }
            );

            // 2) Apply config to model
            model.applyConfig(cfg);

            // 3) Call model asynchronously
            String userText = view.input.getText();
            view.btnGenerate.setDisable(true);
            view.status.setText("Requesting…");
            view.output.clear();

            model.generateAsync(userText).thenAccept(resultText ->
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
    }

    private String selectedTone() {
        if (view.rbInformal.isSelected()) return "informal";
        if (view.rbFormal.isSelected()) return "formal";
        return "neutral";
    }
}