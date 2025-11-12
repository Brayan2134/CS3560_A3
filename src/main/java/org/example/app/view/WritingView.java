package org.example.app.view;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Screen;

import org.example.app.util.Suggestion;
import org.example.app.view.sections.PresetTabsView;
import org.example.app.view.sections.SectionEvents;
import org.example.app.view.sections.TemperatureSection;
import org.example.app.view.sections.ToneSection;
import org.example.app.view.sections.StyleSection;
import org.example.app.view.sections.TextModeSection;
import org.example.app.view.sections.TranslateSection;
import org.example.app.util.*;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TitledPane;

import java.util.List;

/**
 * WritingView — minimal changes to satisfy WritingController and add stats/suggestions.
 * Exposes:
 *  - public fields: btnGenerate, btnResetPreset, inputStats, outputStats, presetTabs
 *  - methods used by controller:
 *    currentPresetKey(), currentPresetInstruction(),
 *    resetActivePresetInstructionToDefault(),
 *    onPresetChanged(), onPresetChanged(String),
 *    setSuggestions(List<Suggestion>),
 *    setTemperature(double), setTone(String), setStyle(String), setTextMode(String), setLanguage(String)
 */
public class WritingView implements SectionEvents {

    // ===== Center editors =====
    public final TextArea input  = new TextArea();
    public final TextArea output = new TextArea();
    private boolean liveStatsWired = false;

    // Stats labels (controller updates these)
    public final Label inputStats  = new Label("Words: 0  Sentences: 0  FRE: -  FKGL: -");
    public final Label outputStats = new Label("Words: 0  Sentences: 0  FRE: -  FKGL: -");

    // Suggestions list (controller fills via setSuggestions)
    private final ListView<Suggestion> suggestionsList = new ListView<>();

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
        input.setPromptText("Type or paste your text here…");
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
        suggestionsBox.setVisible(false); // hide from UI
        suggestionsBox.setManaged(false); // remove from layout sizing so it doesn't reserve space

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

        suggestionsList.setCellFactory(lv -> new ListCell<Suggestion>() {
            @Override protected void updateItem(Suggestion s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setTooltip(null); return; }
                setText(s.getTitle());
                setTooltip(new Tooltip(s.getDetail()));
            }
        });

        // Click a suggestion -> select the offending text in the input area
        suggestionsList.getSelectionModel().selectedItemProperty().addListener((obs, old, s) -> {
            if (s == null) return;
            input.requestFocus();
            input.selectRange(s.getStart(), s.getEnd());
        });

        // Keep suggestions in sync with the input text
        input.textProperty().addListener((obs, old, n) -> refreshSuggestions(n));

        // Initial compute (handles freshly opened view with any existing text)
        refreshSuggestions(input.getText());

        HBox genBox = new HBox(btnGenerate);
        genBox.setSpacing(8);
        rightColumn.getChildren().add(genBox);

        ScrollPane rightScroll = new ScrollPane(rightColumn);
        rightScroll.setFitToWidth(true);
        rightScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // These two actually clamp the BorderPane right region:
        rightScroll.setPrefWidth(420);
        rightScroll.setMaxWidth(420);

        // This is optional, but keeps the viewport consistent
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

    static void clampWidth(Region r, double w) {
        r.setMaxWidth(w);
        r.setPrefWidth(w);
    }

    private VBox labeledBox(String title, Node n) {
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-weight: bold;");
        return new VBox(6, lbl, n);
    }

    // Add this helper
    private Node titled(String title, Node content) {
        if (content instanceof TitledPane) {
            // Section already provides its own titled container — use it directly.
            return content;
        }
        TitledPane tp = new TitledPane(title, content);
        tp.setCollapsible(true);
        tp.setExpanded(true);
        return tp;
    }


    /** Recompute suggestions for the given text and refresh the list. */
    private void refreshSuggestions(String text) {
        suggestionsList.getItems().setAll(SuggestionEngine.suggest(text));
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
        // If your PresetTabsView shows the instruction, ensure it binds to our field
        // or add a method there that sets its text. We avoid calling unknown APIs here.
    }

    /** Optional hook used by controller after preset changes (no-op). */
    public void onPresetChanged() { }

    /** Overload to accept the preset key from controller and update local state. */
    public void onPresetChanged(String key) {
        if (key != null && !key.isBlank()) {
            activePresetKey = key;
        }
    }

    /** Controller pushes computed suggestions into the list. */
    public void setSuggestions(List<Suggestion> suggestions) {
        suggestionsList.getItems().setAll(suggestions);
    }

    // Clears input/output editors
    public void clearEditors() {
        input.clear();
        output.clear();
    }

    // Clears the suggestions list
    public void clearSuggestions() {
        // If you keep a warning label, you can keep the placeholder as-is
        // and just clear the list.
        // suggestionsList.setPlaceholder(new Label("No suggestions"));
        // suggestionsList.getItems().clear();
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
        // default preset
        activePresetKey = "general";
        // knobs + prompt
        resetKnobsToPresetDefaults();

        // editors/suggestions
        clearEditors();
        clearSuggestions();

        input.clear();
        output.clear();
        updateInputStats("");
        updateOutputStats("");

        // stats labels
        inputStats.setText("Words: 0  Sentences: 0  FRE: —  FKGL: —");
        outputStats.setText("Words: 0 Sentences: 0 • FRE: —  FKGL: —");

        status.setText("Session created.");
    }

    // Attach live listeners once when the view is built
    private void wireLiveStats() {
        input.textProperty().addListener((obs, oldV, newV) -> updateInputStats(newV));
        output.textProperty().addListener((obs, oldV, newV) -> updateOutputStats(newV));

        // initialize labels on first show
        updateInputStats(input.getText());
        updateOutputStats(output.getText());
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
        return String.format("Words: %d  •  Sentences: %d  •  FRE: %s  •  FKGL: %s",
                words, sentences, freS, fkglS);
    }

    // Flesch Reading Ease: 206.835 − 1.015*(words/sentences) − 84.6*(syllables/words)
    private Double computeFre(String text) {
        int words = org.example.app.util.TextStats.words(text);
        int sents = org.example.app.util.TextStats.sentences(text);
        int syll  = org.example.app.util.TextStats.syllables(text);
        if (words <= 0 || sents <= 0) return null;
        double wps = (double) words / sents;
        double spw = (double) syll  / words;
        return 206.835 - 1.015 * wps - 84.6 * spw;
    }

    // Flesch–Kincaid Grade Level: 0.39*(words/sentences) + 11.8*(syllables/words) − 15.59
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
}