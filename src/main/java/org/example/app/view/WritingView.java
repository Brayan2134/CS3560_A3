package org.example.app.view;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.app.preset.Presets;

import java.util.LinkedHashMap;
import java.util.Map;

public class WritingView {

    private final BorderPane root = new BorderPane();

    // Shared Input/Output (left)
    public final TextArea input = new TextArea();
    public final TextArea output = new TextArea();

    // Right: controls
    public final Slider temperature = new Slider(0, 2, 0.5);
    public final ToggleGroup toneGroup = new ToggleGroup();
    public final RadioButton rbInformal = new RadioButton("Informal");
    public final RadioButton rbNeutral = new RadioButton("Neutral");
    public final RadioButton rbFormal = new RadioButton("Formal");
    public final ComboBox<String> style = new ComboBox<>();
    public final ComboBox<String> textMode = new ComboBox<>();

    // Translation control
    public final ComboBox<String> translateLang = new ComboBox<>();

    public final Button btnGenerate = new Button("Generate");
    public final Button btnResetPreset = new Button("Reset to preset");
    public final Label status = new Label("Idle");

    // Tabs: one per preset
    public final TabPane tabs = new TabPane();
    private final Map<String, TextArea> presetInstructionEditors = new LinkedHashMap<>();

    // Toggleable sections
    public TitledPane stylePane;
    public TitledPane textModePane;
    public TitledPane translatePane;

    public WritingView() {
        // Text areas (bigger defaults + wrapping)
        input.setPromptText("Type your text here…");
        output.setEditable(false);
        input.setWrapText(true);
        output.setWrapText(true);
        input.setPrefRowCount(18);
        output.setPrefRowCount(18);

        // Build IO area with wrappers that are allowed to grow
        VBox leftBox  = (VBox) wrap("Input", input);
        VBox rightBox = (VBox) wrap("Output", output);

        HBox io = new HBox(10, leftBox, rightBox);
        io.setPadding(new Insets(10));

        // ✅ Let the two columns grow inside the HBox
        HBox.setHgrow(leftBox, Priority.ALWAYS);
        HBox.setHgrow(rightBox, Priority.ALWAYS);

        // ✅ Let the IO row grow inside the center VBox
        VBox center = new VBox(8, io);
        VBox.setVgrow(io, Priority.ALWAYS);
        center.setPadding(new Insets(6));

        // Preset tabs (editable instruction boxes)
        buildPresetTabs();

        // Tone radios
        rbInformal.setToggleGroup(toneGroup);
        rbNeutral.setToggleGroup(toneGroup);
        rbFormal.setToggleGroup(toneGroup);
        rbNeutral.setSelected(true);

        // Style + Text mode
        style.getItems().addAll("none", "APA", "MLA", "Chicago");
        style.getSelectionModel().select("none");

        textMode.getItems().addAll("summarize", "same", "expand");
        textMode.getSelectionModel().select("same");

        // Translation dropdown
        translateLang.getItems().addAll("English", "Spanish", "French", "Italian", "Japanese", "Mandarin", "Korean");
        translateLang.getSelectionModel().select("English");

        // Temperature slider + custom axis + tooltip + live value
        temperature.setBlockIncrement(0.1);
        temperature.setMajorTickUnit(0.5);
        temperature.setShowTickMarks(true);
        temperature.setShowTickLabels(false);

        Tooltip tempTip = new Tooltip(
                "Temperature controls randomness/creativity:\n" +
                        "• 0.0 = deterministic (precise, consistent)\n" +
                        "• 0.2–0.4 = formal/professional writing\n" +
                        "• ~0.5 = balanced\n" +
                        "• 0.6–0.8 = creative/varied phrasing\n" +
                        "• 1.0+ = very playful, more surprising\n" +
                        "Tip: lower for summaries & formal docs; higher for brainstorming."
        );
        Tooltip.install(temperature, tempTip);

        Label tempValue = new Label();
        tempValue.textProperty().bind(Bindings.format("Current: %.2f", temperature.valueProperty()));
        Label leftLbl  = new Label("0 (more focused)");
        Label midLbl   = new Label("balanced");
        Label rightLbl = new Label("2 (more creative)");
        Region spacerL = new Region(); HBox.setHgrow(spacerL, Priority.ALWAYS);
        Region spacerR = new Region(); HBox.setHgrow(spacerR, Priority.ALWAYS);
        HBox axis = new HBox(8, leftLbl, spacerL, midLbl, spacerR, rightLbl);
        axis.setAlignment(Pos.BASELINE_LEFT);
        VBox tempBox = new VBox(6, temperature, axis, tempValue);

        // Build toggleable sections as titled panes so we can show/hide them
        TitledPane tempPane = titled("Temperature", tempBox);
        TitledPane tonePane = titled("Tone", new VBox(6, rbInformal, rbNeutral, rbFormal));
        stylePane = titled("Style", style);
        textModePane = titled("Text mode", textMode);
        translatePane = titled("Translate", translateLang);

        // Widen the presets area and right column (so 5 tabs fit nicely)
        tabs.setTabMinWidth(100);
        tabs.setTabMaxWidth(Double.MAX_VALUE);
        tabs.setPrefWidth(640);
        tabs.setMinWidth(640);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        VBox right = new VBox(12,
                new Label("Presets"),
                tabs,
                btnResetPreset,
                new Separator(),
                tempPane,
                tonePane,
                stylePane,
                textModePane,
                translatePane,
                btnGenerate,
                new Separator(),
                status
        );
        right.setAlignment(Pos.TOP_LEFT);
        right.setPadding(new Insets(12));
        right.setPrefWidth(640);

        // Layout
        root.setCenter(center);
        root.setRight(right);
    }

    private void buildPresetTabs() {
        presetInstructionEditors.clear();
        tabs.getTabs().clear();

        Presets.all().forEach((key, preset) -> {
            TextArea instruction = new TextArea(preset.defaultInstruction().trim());
            instruction.setWrapText(true);
            instruction.setPrefRowCount(10);
            instruction.setPrefColumnCount(60);
            VBox.setVgrow(instruction, Priority.ALWAYS);

            VBox content = new VBox(8,
                    new Label("Preset Prompt (editable):"),
                    instruction
            );
            content.setPadding(new Insets(10));
            Tab tab = new Tab(preset.title(), content);
            tab.setId(preset.key());
            tab.setClosable(false);

            tabs.getTabs().add(tab);
            presetInstructionEditors.put(key, instruction);
        });

        if (!tabs.getTabs().isEmpty()) {
            tabs.getSelectionModel().select(0);
        }
    }

    /** Current preset key (e.g., "general", "creative", etc.). */
    public String currentPresetKey() {
        Tab t = tabs.getSelectionModel().getSelectedItem();
        return t != null ? t.getId() : Presets.GENERAL;
    }

    /** Current instruction text for the active preset tab. */
    public String currentPresetInstruction() {
        String key = currentPresetKey();
        TextArea ta = presetInstructionEditors.get(key);
        return ta != null ? ta.getText() : "";
    }

    /** Reset the active tab's instruction to its default. */
    public void resetActivePresetInstructionToDefault() {
        String key = currentPresetKey();
        var p = Presets.all().get(key);
        if (p != null) {
            TextArea ta = presetInstructionEditors.get(key);
            if (ta != null) ta.setText(p.defaultInstruction().trim());
        }
    }

    // ——— helpers ———
    private Node wrap(String title, Node n) {
        VBox box = new VBox(6, new Label(title), n);
        // Let the inner control grow to fill vertical space
        VBox.setVgrow(n, Priority.ALWAYS);
        // Important: the wrapper VBox must be allowed to grow inside its parent HBox
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private TitledPane titled(String title, Node content) {
        TitledPane tp = new TitledPane(title, content);
        tp.setExpanded(true);
        return tp;
    }

    // Public helpers so controller can toggle sections
    public void setStyleVisible(boolean visible) {
        stylePane.setManaged(visible);
        stylePane.setVisible(visible);
    }
    public void setTextModeVisible(boolean visible) {
        textModePane.setManaged(visible);
        textModePane.setVisible(visible);
    }
    public void setTranslationVisible(boolean visible) {
        translatePane.setManaged(visible);
        translatePane.setVisible(visible);
    }

    public BorderPane getRoot() { return root; }
}