package org.example.app.view;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Screen;

import org.example.app.suggest.SuggestionIssue;
import org.example.app.view.sections.PresetTabsView;
import org.example.app.view.sections.SectionEvents;
import org.example.app.view.sections.TemperatureSection;
import org.example.app.view.sections.ToneSection;
import org.example.app.view.sections.StyleSection;
import org.example.app.view.sections.TextModeSection;
import org.example.app.view.sections.TranslateSection;

import java.util.List;
import java.util.stream.Collectors;

/**
 * WritingView — minimal changes to satisfy WritingController and add stats/suggestions.
 * Exposes:
 *  - public fields: btnGenerate, btnResetPreset, inputStats, outputStats, presetTabs
 *  - methods used by controller:
 *    currentPresetKey(), currentPresetInstruction(),
 *    resetActivePresetInstructionToDefault(),
 *    onPresetChanged(), onPresetChanged(String),
 *    setSuggestions(List<...>)  (controller no longer uses this; showSuggestions is preferred),
 *    setTemperature(double), setTone(String), setStyle(String), setTextMode(String), setLanguage(String)
 */
public class WritingView implements SectionEvents {

    // ===== Center editors =====
    public final TextArea input  = new TextArea();
    public final TextArea output = new TextArea();
    private boolean liveStatsWired = false;

    // Stats labels (controller updates these)
    public final Label inputStats  = new Label("Words: 0   Sentences: 0   FRE: -   FKGL: -");
    public final Label outputStats = new Label("Words: 0   Sentences: 0   FRE: -   FKGL: -");

    // Suggestions list (now just strings for display)
    private final ListView<String> suggestionsList = new ListView<>();

    // Status + actions
    public final Label  status = new Label("Ready.");
    public final Button btnGenerate    = new Button("Generate");
    public final Button btnResetPreset = new Button("Reset to preset");

    // ===== Presets / sections =====
    public final PresetTabsView presetTabs;

    private final TemperatureSection temperatureSection;
    private final ToneSection        toneSection;
    private final StyleSection       styleSection;
    private final TextModeSection    textModeSection;
    private final TranslateSection   translateSection;

    // View-level state (fed by callbacks or controller)
    private double currentTemperature = 0.5;
    private String currentTone        = "neutral";
    private String currentStyle       = "none";
    private String currentTextMode    = "same";
    private String currentLanguage    = "English";

    // Preset state (controller may set/change these)
    private String activePresetKey = "general";
    private String activePresetInstruction =
            "You are a helpful assistant. Improve clarity while preserving meaning.";

    private final BorderPane root = new BorderPane();

    public WritingView() {
        // Prefer large default window; clamp to screen bounds
        double prefW = 1920, prefH = 1080;
        var b = Screen.getPrimary().getBounds();
        prefW = Math.min(prefW, Math.max(1280, b.getWidth()  - 60));
        prefH = Math.min(prefH, Math.max( 720, b.getHeight() - 80));

        // ---- Editors ----
        input.setWrapText(true);
        output.setWrapText(true);
        input.setPromptText("Type or paste your text here!");
        output.setEditable(false);

        // Ctrl+Enter triggers Generate
        input.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER && e.isControlDown() && !btnGenerate.isDisabled()) {
                btnGenerate.fire();
                e.consume();
            }
        });

        // Wrap editors to show stats labels below
        VBox leftBox   = new VBox(6, input,  inputStats);
        VBox centerBox = new VBox(6, output, outputStats);
        VBox.setVgrow(input,  Priority.ALWAYS);
        VBox.setVgrow(output, Priority.ALWAYS);

        SplitPane editors = new SplitPane(leftBox, centerBox);
        editors.setOrientation(Orientation.HORIZONTAL);
        editors.setDividerPositions(0.5);
        BorderPane.setMargin(editors, new Insets(10));
        root.setCenter(editors);

        // ---- Right side: presets + sections + suggestions + generate ----
        presetTabs = new PresetTabsView(); // no assumptions about its API

        temperatureSection = new TemperatureSection(this);
        toneSection        = new ToneSection(this);
        styleSection       = new StyleSection(this);
        textModeSection    = new TextModeSection(this);
        translateSection   = new TranslateSection(this);

        VBox rightColumn = new VBox(10);
        rightColumn.setPadding(new Insets(10));
        rightColumn.setFillWidth(true);
        rightColumn.setMaxWidth(420);  // clamp right pane width

        // Wrap suggestions UI into a container we can hide without breaking functionality
        VBox suggestionsBox = new VBox(6, new Label("Suggestions"), suggestionsList);
        suggestionsBox.setVisible(true); // hidden by default (you can flip to true later)
        suggestionsBox.setManaged(true); // don't reserve layout space while hidden

        Node presetTabsNode = presetTabs.getNode();

        rightColumn.getChildren().addAll(
                labeledBox("Presets", presetTabsNode),
                btnResetPreset,
                titled("Temperature", temperatureSection.getNode()),
                titled("Tone",         toneSection.getNode()),
                titled("Style",        styleSection.getNode()),
                titled("Text mode",    textModeSection.getNode()),
                titled("Translate",    translateSection.getNode())
        );

        suggestionsList.setPlaceholder(new Label("No suggestions"));
        suggestionsList.setPrefHeight(150);
        rightColumn.getChildren().add(suggestionsBox);

        // Clicking a row selects the span IN THE INPUT editor.
        suggestionsList.setOnMouseClicked(e -> {
            int idx = suggestionsList.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < suggestionsList.getItems().size()) {
                // The controller calls showSuggestions(...) which keeps its own mapping
                // via a captured list; selection is handled there. Here we just ensure
                // the control exists and can be focused.
                input.requestFocus();
            }
        });

        HBox genBox = new HBox(btnGenerate);
        genBox.setSpacing(8);
        rightColumn.getChildren().add(genBox);

        ScrollPane rightScroll = new ScrollPane(rightColumn);
        rightScroll.setFitToWidth(true);
        rightScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rightScroll.setPrefWidth(420);
        rightScroll.setMaxWidth(420);
        rightScroll.setPrefViewportWidth(420);

        root.setRight(rightScroll);

        // Bottom status
        HBox statusBar = new HBox(status);
        statusBar.setPadding(new Insets(6, 10, 6, 10));
        statusBar.setStyle("""
                -fx-background-color: -fx-control-inner-background;
                -fx-border-color: -fx-box-border;
                -fx-border-width: 1 0 0 0;
                """);
        root.setBottom(statusBar);

        root.setPrefSize(prefW, prefH);

        wireLiveStatsOnce();
    }

    private void wireLiveStatsOnce() {
        if (liveStatsWired) return;     // prevents duplicate listeners
        liveStatsWired = true;

        input.textProperty().addListener((obs, o, n) -> updateInputStats(n));
        output.textProperty().addListener((obs, o, n) -> updateOutputStats(n));

        // initialize labels so they aren't blank on first show
        updateInputStats(input.getText());
        updateOutputStats(output.getText());
    }

    // ===== helpers =====

    /** Listen for changes in the input editor (for debounced checks). */
    public javafx.beans.property.StringProperty inputTextProperty() {
        return input.textProperty();         // <-- input, not output
    }

    /** Render suggestion list and select spans in the INPUT editor only. Called by controller. */
    public void showSuggestions(List<SuggestionIssue> issues) {
        if (issues == null) issues = List.of();

        // Build rows for the ListView (Java 11 friendly).
        List<String> rows = issues.stream()
                .map(i -> String.format("[%s] %s (%d..%d)", i.type, i.message, i.start, i.end))
                .collect(Collectors.toList());

        suggestionsList.getItems().setAll(rows.toArray(new String[0]));

        // Capture for selection mapping inside the handler
        final List<SuggestionIssue> finalIssues = issues;

        // Clicking a row selects its span IN THE INPUT editor.
        suggestionsList.setOnMouseClicked(e -> {
            int idx = suggestionsList.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < finalIssues.size()) {
                SuggestionIssue iss = finalIssues.get(idx);

                // Clamp to input bounds
                int start = Math.max(0, Math.min(iss.start, input.getLength()));
                int end   = Math.max(start, Math.min(iss.end,   input.getLength()));

                input.requestFocus();
                input.selectRange(start, end);
            }
        });
    }

    static void clampWidth(Region r, double w) {
        r.setMaxWidth(w);
        r.setPrefWidth(w);
    }

    private VBox labeledBox(String title, Node n) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-weight: bold;");
        return new VBox(6, lbl, n);
    }

    private Node titled(String title, Node content) {
        if (content instanceof TitledPane) {
            return content;
        }
        TitledPane tp = new TitledPane(title, content);
        tp.setCollapsible(true);
        tp.setExpanded(true);
        return tp;
    }

    // ===== SectionEvents callbacks (update local state) =====
    public void onTemperatureChanged(double value) { currentTemperature = value; }
    public void onToneChanged(String toneKey)      { if (toneKey != null && !toneKey.isBlank())   currentTone = toneKey; }
    public void onStyleChanged(String styleKey)    { if (styleKey != null && !styleKey.isBlank()) currentStyle = styleKey; }
    public void onTextModeChanged(String modeKey)  { if (modeKey != null && !modeKey.isBlank())   currentTextMode = modeKey; }
    public void onLanguageChanged(String language) { if (language != null && !language.isBlank()) currentLanguage = language; }

    // ===== Methods your controller calls directly =====
    public String currentPresetKey() { return activePresetKey; }
    public String currentPresetInstruction() { return activePresetInstruction; }

    /** Reset the editable instruction to a default for the active preset. */
    public void resetActivePresetInstructionToDefault() {
        activePresetInstruction = switch (activePresetKey) {
            case "creative"     -> "Write vividly with strong imagery and narrative flow.";
            case "professional" -> "Write concisely with a professional, objective tone.";
            case "academic"     -> "Write formally with clear structure and precise citations.";
            case "code"         -> "Produce concise code documentation and examples.";
            default             -> "You are a helpful assistant. Improve clarity while preserving meaning.";
        };
    }

    /** Optional hook used by controller after preset changes (no-op). */
    public void onPresetChanged() { }

    /** Overload to accept the preset key from controller and update local state. */
    public void onPresetChanged(String key) {
        if (key != null && !key.isBlank()) {
            activePresetKey = key;
        }
    }

    /** (Legacy) allow controller to push prebuilt suggestion rows if needed. */
    public void setSuggestions(List<?> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            suggestionsList.getItems().clear();
            return;
        }
        // If controller pushes strings, accept them; otherwise fall back to toString()
        if (!suggestions.isEmpty() && suggestions.get(0) instanceof String) {
            @SuppressWarnings("unchecked")
            List<String> rows = (List<String>) suggestions;
            suggestionsList.getItems().setAll(rows);
        } else {
            suggestionsList.getItems().setAll(
                    suggestions.stream().map(Object::toString).collect(Collectors.toList())
            );
        }
    }

    // Clears input/output editors
    public void clearEditors() {
        input.clear();
        output.clear();
    }

    // Clears the suggestions list
    public void clearSuggestions() {
        suggestionsList.getItems().clear();
    }

    // Resets the knobs/toggles to defaults AND the preset instruction
    public void resetKnobsToPresetDefaults() {
        setTemperature(0.5);
        setTone("neutral");
        setStyle("none");
        setTextMode("same");
        setLanguage("English");
        resetActivePresetInstructionToDefault();
    }

    // Full new-session reset (used by controller)
    public void resetForNewSession() {
        activePresetKey = "general";
        resetKnobsToPresetDefaults();
        clearEditors();
        clearSuggestions();
        updateInputStats("");
        updateOutputStats("");
        inputStats.setText("Words: 0    Sentences: 0    FRE: -    FKGL: -");
        outputStats.setText("Words: 0   Sentences: 0   FRE: -  FKGL: -");
        status.setText("Session created.");
    }

    private void updateInputStats(String text) {
        int words     = org.example.app.util.TextStats.words(text);
        int sentences = org.example.app.util.TextStats.sentences(text);
        Double fre    = computeFre(text);
        Double fkgl   = computeFkgl(text);
        inputStats.setText(formatStats(words, sentences, fre, fkgl));
    }

    private void updateOutputStats(String text) {
        int words     = org.example.app.util.TextStats.words(text);
        int sentences = org.example.app.util.TextStats.sentences(text);
        Double fre    = computeFre(text);
        Double fkgl   = computeFkgl(text);
        outputStats.setText(formatStats(words, sentences, fre, fkgl));
    }

    private String formatStats(int words, int sentences, Double fre, Double fkgl) {
        String freS  = (fre  == null ? "—" : String.format("%.1f", fre));
        String fkglS = (fkgl == null ? "—" : String.format("%.1f", fkgl));
        return String.format("Words: %d   Sentences: %d   FRE: %s   FKGL: %s",
                words, sentences, freS, fkglS);
    }

    // Flesch Reading Ease
    private Double computeFre(String text) {
        int words = org.example.app.util.TextStats.words(text);
        int sents = org.example.app.util.TextStats.sentences(text);
        int syll  = org.example.app.util.TextStats.syllables(text);
        if (words <= 0 || sents <= 0) return null;
        double wps = (double) words / sents;
        double spw = (double) syll  / words;
        return 206.835 - 1.015 * wps - 84.6 * spw;
    }

    // Flesch–Kincaid Grade Level
    private Double computeFkgl(String text) {
        int words = org.example.app.util.TextStats.words(text);
        int sents = org.example.app.util.TextStats.sentences(text);
        int syll  = org.example.app.util.TextStats.syllables(text);
        if (words <= 0 || sents <= 0) return null;
        double wps = (double) words / sents;
        double spw = (double) syll  / words;
        return 0.39 * wps + 11.8 * spw - 15.59;
    }

    // ===== setters the controller expects =====
    public void setTemperature(double v) { currentTemperature = v; }
    public void setTone(String key)      { if (key != null) currentTone = key; }
    public void setStyle(String key)     { if (key != null) currentStyle = key; }
    public void setTextMode(String key)  { if (key != null) currentTextMode = key; }
    public void setLanguage(String lang) { if (lang != null) currentLanguage = lang; }

    // ===== getters used by controller =====
    public BorderPane getRoot() { return root; }
    public double getTemperature() { return currentTemperature; }
    public String getToneKey()     { return currentTone; }
    public String getStyleKey()    { return currentStyle; }
    public String getTextModeKey() { return currentTextMode; }
    public String getLanguage()    { return currentLanguage; }
    public String getInputText()   { return input.getText(); }
}