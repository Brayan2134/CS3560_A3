package org.example.app.view;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.app.preset.Preset;
import org.example.app.preset.PresetRegistry;
import org.example.app.view.sections.*;

import java.util.List;

public class WritingView implements SectionEvents {

    private final BorderPane root = new BorderPane();

    // IO
    public final TextArea input = new TextArea();
    public final TextArea output = new TextArea();
    public final Button btnGenerate = new Button("Generate");
    public final Button btnResetPreset = new Button("Reset to preset");
    public final Label status = new Label("Idle");

    // Tabs
    public final PresetTabsView presetTabs = new PresetTabsView();

    // Dynamic sections (references for controller)
    private TemperatureSection temperatureSection;
    private ToneSection toneSection;
    private StyleSection styleSection;
    private TextModeSection textModeSection;
    private TranslateSection translateSection;

    private final VBox right = new VBox(12);

    public WritingView() {
        // left IO
        input.setPromptText("Type your text here…");
        output.setEditable(false);
        input.setWrapText(true);
        output.setWrapText(true);
        input.setPrefRowCount(18);
        output.setPrefRowCount(18);

        VBox leftBox  = wrap("Input", input);
        VBox rightBox = wrap("Output", output);
        HBox io = new HBox(10, leftBox, rightBox);
        io.setPadding(new Insets(10));
        HBox.setHgrow(leftBox, Priority.ALWAYS);
        HBox.setHgrow(rightBox, Priority.ALWAYS);

        VBox center = new VBox(8, io);
        VBox.setVgrow(io, Priority.ALWAYS);
        center.setPadding(new Insets(6));

        // right column scaffold
        right.setPadding(new Insets(12));
        right.setPrefWidth(640);
        right.getChildren().addAll(new Label("Presets"), presetTabs.getNode(), btnResetPreset, new Separator());
        VBox.setVgrow(presetTabs.getNode(), Priority.ALWAYS);

        root.setCenter(center);
        root.setRight(right);

        // initial build for first preset
        rebuildRightColumnForPreset(presetTabs.currentKey());
    }

    /** Controller calls this when tab changes. */
    public void onPresetChanged(String presetKey) {
        rebuildRightColumnForPreset(presetKey);
    }

    private void rebuildRightColumnForPreset(String key) {
        // Look up preset + capabilities
        Preset preset = PresetRegistry.all().get(key);
        var caps = preset.capabilities();

        // 1) Clear the column entirely to avoid duplicate-child errors
        right.getChildren().clear();

        // 2) Rebuild the static header
        Label header = new Label("Presets");
        right.getChildren().addAll(header, presetTabs.getNode(), btnResetPreset, new Separator());

        // 3) Build dynamic sections based on capabilities
        List<SectionView> sections = SectionFactory.buildRightColumn(
                this, caps.showStyle(), caps.showTextMode(), caps.showTranslation()
        );

        // Keep typed references for getters/setters
        temperatureSection = sections.stream().filter(s -> s instanceof TemperatureSection)
                .map(s -> (TemperatureSection) s).findFirst().orElse(null);
        toneSection = sections.stream().filter(s -> s instanceof ToneSection)
                .map(s -> (ToneSection) s).findFirst().orElse(null);
        styleSection = sections.stream().filter(s -> s instanceof StyleSection)
                .map(s -> (StyleSection) s).findFirst().orElse(null);
        textModeSection = sections.stream().filter(s -> s instanceof TextModeSection)
                .map(s -> (TextModeSection) s).findFirst().orElse(null);
        translateSection = sections.stream().filter(s -> s instanceof TranslateSection)
                .map(s -> (TranslateSection) s).findFirst().orElse(null);

        // Add the sections
        for (SectionView s : sections) {
            right.getChildren().add(s.getNode());
        }

        // 4) Footer (fresh Separator)
        right.getChildren().addAll(new Separator(), btnGenerate, new Separator(), status);

        // Maintain widths
        right.setPrefWidth(640);
        VBox.setVgrow(presetTabs.getNode(), Priority.ALWAYS);
    }

    // getters for controller
    public String currentPresetKey() { return presetTabs.currentKey(); }
    public String currentPresetInstruction() { return presetTabs.currentInstruction(); }
    public void resetActivePresetInstructionToDefault() { presetTabs.resetCurrentToDefault(); }

    public double getTemperature() { return temperatureSection != null ? temperatureSection.get() : 0.5; }
    public void setTemperature(double v) { if (temperatureSection != null) temperatureSection.set(v); }
    public void setTone(String key) { if (toneSection != null) toneSection.select(key); }
    public void setStyle(String key) { if (styleSection != null) styleSection.select(key); }
    public void setTextMode(String key) { if (textModeSection != null) textModeSection.select(key); }
    public void setLanguage(String lang) { if (translateSection != null) translateSection.select(lang); }

    private VBox wrap(String title, Node n) {
        VBox box = new VBox(6, new Label(title), n);
        VBox.setVgrow(n, Priority.ALWAYS);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    public BorderPane getRoot() { return root; }

    // SectionEvents (optional live reactions)
    @Override public void onTemperatureChanged(double value) {}
    @Override public void onToneChanged(String toneKey) {}
    @Override public void onStyleChanged(String styleKey) {}
    @Override public void onTextModeChanged(String modeKey) {}
    @Override public void onLanguageChanged(String language) {}

    // === getters used by the controller ===
    public String getToneKey() {
        return (toneSection != null) ? toneSection.getSelectedKey() : "neutral";
    }
    public String getStyleKey() {
        return (styleSection != null) ? styleSection.getSelected() : "none";
    }
    public String getTextModeKey() {
        return (textModeSection != null) ? textModeSection.getSelected() : "same";
    }
    public String getLanguage() {
        return (translateSection != null) ? translateSection.getSelected() : "English";
    }


}