package org.example.app.controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;
import org.example.app.model.WritingConfig;
import org.example.app.model.WritingModel;
import org.example.app.preset.Preset;
import org.example.app.preset.PresetCapabilities;
import org.example.app.preset.PresetRegistry;
import org.example.app.suggest.*;
import org.example.app.suggest.SuggestionEngine;
import org.example.app.view.WritingView;
import org.example.app.persistence.*;
import org.example.app.util.*;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.util.concurrent.TimeUnit;

import javafx.application.Platform;


public class WritingController {
    private final PauseTransition inputDebounce = new PauseTransition(Duration.millis(150));
    private final PauseTransition outputDebounce = new PauseTransition(Duration.millis(150));
    private final WritingModel model;
    private final WritingView view;

    // persistence roots
    private final Path sessionsRoot = Paths.get(System.getProperty("user.home"),
            ".writing-assistant", "sessions");
    private final SessionManager sessions = new SessionManager(sessionsRoot);
    private final LastSessionStore lastSession =
            new LastSessionStore(Paths.get(System.getProperty("user.home"),
                    ".writing-assistant", "last-session.json"));
    private String currentSessionId;

    private final SuggestionEngine suggest;
    private final ScheduledExecutorService suggestScheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pendingSuggest;
    private final ChangeListener<String> onInputChanged = (obs, oldV, newV) -> debounceSuggest(newV);


    // shortcuts
    private static final KeyCombination SAVE_KC = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
    private static final KeyCombination HISTORY_KC = new KeyCodeCombination(KeyCode.H, KeyCombination.CONTROL_DOWN);
    private static final KeyCombination NEW_KC = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
    private static final KeyCombination OPEN_KC = new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN);
    private static final KeyCombination EXPORT_KC = new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN);

    public WritingController(WritingModel model, WritingView view) {
        this.model = model;
        this.view = view;

        // NEW: default engine (swap later for a real provider)
        this.suggest = new CompositeSuggestionEngine(new LanguageToolSuggestionEngine("en-US"));

        wireUI();
        openOrCreateSession();
        applyPresetDefaults(view.currentPresetKey());

        // NEW: start listening + first kick
        this.view.inputTextProperty().addListener(onInputChanged);
        debounceSuggest(this.view.getInputText());
    }

    private void debounceSuggest(String text) {
        if (pendingSuggest != null) pendingSuggest.cancel(false);
        pendingSuggest = suggestScheduler.schedule(() -> runSuggest(text), 250, TimeUnit.MILLISECONDS);
    }

    private void runSuggest(String text) {
        try {
            var req = SuggestionRequest.of(text == null ? "" : text);
            suggest.checkAsync(req).thenAccept(result ->
                    Platform.runLater(() -> view.showSuggestions(result.issues))
            );
        } catch (Throwable t) {
            Platform.runLater(() -> view.showSuggestions(java.util.List.of()));
        }
    }

    private void wireStatsAndSuggestions() {
        view.input.textProperty().addListener((obs, o, n) -> {
            inputDebounce.setOnFinished(e -> {
                updateStats(n, true);
                updateSuggestions(n);
            });
            inputDebounce.playFromStart();
        });
        view.output.textProperty().addListener((obs, o, n) -> {
            outputDebounce.setOnFinished(e -> updateStats(n, false));
            outputDebounce.playFromStart();
        });
        // initial
        updateStats(view.input.getText(), true);
        updateStats(view.output.getText(), false);
        updateSuggestions(view.input.getText());
    }

    private void updateSuggestions(String txt) {
        var req = SuggestionRequest.of(txt == null ? "" : txt);
        suggest.checkAsync(req).thenAccept(result ->
                Platform.runLater(() -> view.showSuggestions(result.issues))
        );
    }

    private void updateStats(String txt, boolean forInput) {
        int w = TextStats.words(txt);
        int s = TextStats.sentences(txt);
        double fre = TextStats.fleschReadingEase(txt);
        double fk  = TextStats.fkGradeLevel(txt);
        String msg = String.format("Words: %d • Sentences: %d • FRE: %.1f • FKGL: %.1f", w, s, fre, fk);
        if (forInput) view.inputStats.setText(msg); else view.outputStats.setText(msg);
    }

    private void wireUI() {
        // Generate
        view.btnGenerate.setOnAction(e -> onGenerate());

        // Reset
        view.btnResetPreset.setOnAction(e -> {
            view.resetActivePresetInstructionToDefault();
            applyPresetDefaults(view.currentPresetKey());
        });

        // Tab change
        ChangeListener<Tab> tabListener = (obs, oldTab, newTab) -> {
            String key = view.currentPresetKey();
            view.onPresetChanged(key);
            applyPresetDefaults(key);
        };
        view.presetTabs.getTabs().getSelectionModel().selectedItemProperty().addListener(tabListener);

        // Menu
        installMenu();

        // Keystrokes (root)
        Node root = view.getRoot();
        root.setOnKeyPressed(ev -> {
            if (SAVE_KC.match(ev)) { saveCurrentRevision("Ctrl+S"); ev.consume(); }
            else if (HISTORY_KC.match(ev)) { showHistoryDialog(); ev.consume(); }
            else if (NEW_KC.match(ev)) { newSession(); ev.consume(); }
            else if (OPEN_KC.match(ev)) { openSessionChooser(); ev.consume(); }
            else if (EXPORT_KC.match(ev)) { exportChooser(); ev.consume(); }
        });
    }

    private void installMenu() {
        Menu file = new Menu("File");
        MenuItem mNew = new MenuItem("New Session (Ctrl+N)");
        MenuItem mOpen = new MenuItem("Open Session… (Ctrl+O)");
        MenuItem mSave = new MenuItem("Save Revision (Ctrl+S)");
        MenuItem mHist = new MenuItem("History (Ctrl+H)");
        MenuItem mExport = new MenuItem("Export… (Ctrl+E)");
        file.getItems().addAll(mNew, mOpen, new SeparatorMenuItem(), mSave, mHist, new SeparatorMenuItem(), mExport);

        mNew.setOnAction(e -> newSession());
        mOpen.setOnAction(e -> openSessionChooser());
        mSave.setOnAction(e -> saveCurrentRevision("Menu Save"));
        mHist.setOnAction(e -> showHistoryDialog());
        mExport.setOnAction(e -> exportChooser());

        MenuBar bar = new MenuBar(file);
        view.getRoot().setTop(bar);  // simple; if you already have a top node, wrap in VBox
    }

    // ---------- startup: try last session, else choose, else create ----------
    private void openOrCreateSession() {
        try {
            String last = lastSession.read();
            if (last != null && !last.isBlank()) {
                // validate it exists
                List<SessionInfo> list = sessions.listSessions();
                boolean exists = list.stream().anyMatch(si -> si.sessionId.equals(last));
                if (exists) {
                    currentSessionId = last;
                    view.status.setText("Reopened last session.");
                    // load latest snapshot if any
                    SessionSnapshot snap = sessions.loadLatest(currentSessionId);
                    if (snap != null) applySnapshotToView(snap);
                    return;
                }
            }
            // If there are existing sessions, offer to open one
            List<SessionInfo> existing = sessions.listSessions();
            if (!existing.isEmpty()) {
                Alert ask = new Alert(Alert.AlertType.CONFIRMATION,
                        "Open existing session or create a new one?",
                        new ButtonType("Open…", ButtonBar.ButtonData.OK_DONE),
                        new ButtonType("New", ButtonBar.ButtonData.APPLY));
                ask.setTitle("Start");
                Optional<ButtonType> ch = ask.showAndWait();
                if (ch.isPresent() && ch.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                    openSessionChooser();
                    return;
                }
            }
            // fallback: create new
            newSession();
        } catch (Exception e) {
            view.status.setText("Could not open session: " + e.getMessage());
            newSession();
        }
    }

    private void newSession() {
        try {
            currentSessionId = sessions.createSession("Session " + Instant.now());
            view.resetForNewSession();
            lastSession.write(currentSessionId);
            view.status.setText("New session created.");
        } catch (Exception e) {
            view.status.setText("Failed to create session: " + e.getMessage());
        }
    }

    private void openSessionChooser() {
        try {
            List<SessionInfo> list = sessions.listSessions();
            if (list.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "No sessions found. Creating a new one.").showAndWait();
                newSession();
                return;
            }

            // Default to the first (or most recent if you prefer to sort)
            ChoiceDialog<SessionInfo> chooser = new ChoiceDialog<>(list.get(0), list);
            chooser.setTitle("Open Session");
            chooser.setHeaderText("Choose a session to open");
            chooser.setContentText("Sessions:");

            Optional<SessionInfo> chosen = chooser.showAndWait();
            if (chosen.isEmpty()) return;

            currentSessionId = chosen.get().sessionId;
            lastSession.write(currentSessionId);

            SessionSnapshot snap = sessions.loadLatest(currentSessionId);
            if (snap != null) applySnapshotToView(snap);
            view.status.setText("Opened session.");
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Open failed: " + e.getMessage()).showAndWait();
        }
    }

    // ----------------- GENERATE (unchanged from earlier except helpers) -----------------
    private void onGenerate() {
        String presetKey = view.currentPresetKey();
        Preset preset = PresetRegistry.all().getOrDefault(
                presetKey, PresetRegistry.all().values().iterator().next()
        );
        PresetCapabilities caps = preset.capabilities();

        double temperature = view.getTemperature();
        WritingConfig.Tone tone = switch (safe(view.getToneKey())) {
            case "informal" -> WritingConfig.Tone.INFORMAL;
            case "formal"   -> WritingConfig.Tone.FORMAL;
            default         -> WritingConfig.Tone.NEUTRAL;
        };
        String grammarStyle = caps.showStyle() ? safe(view.getStyleKey()) : "none";
        WritingConfig.TextMode textMode =
                caps.showTextMode()
                        ? switch (safe(view.getTextModeKey())) {
                    case "summarize" -> WritingConfig.TextMode.SUMMARIZE;
                    case "expand"    -> WritingConfig.TextMode.EXPAND;
                    default          -> WritingConfig.TextMode.SAME;
                }
                        : WritingConfig.TextMode.SUMMARIZE;

        WritingConfig cfg = new WritingConfig("gpt-4o-mini", temperature, 800, tone, grammarStyle, textMode);
        model.applyConfig(cfg);

        String language = caps.showTranslation() ? safe(view.getLanguage()) : "English";
        String transInstruction = (!"English".equalsIgnoreCase(language)) ? "Write the final output in " + language + "." : "";
        String instruction = safe(view.currentPresetInstruction()).trim();
        String userText = safe(view.input.getText());

        String header = combineNonBlank(transInstruction, instruction);
        String finalPrompt = header.isBlank() ? userText : header + "\n\n---\n\n" + userText;

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

    private void applyPresetDefaults(String presetKey) {
        var all = PresetRegistry.all();
        Preset preset = all.getOrDefault(presetKey, all.values().iterator().next());
        var d = preset.defaults();

        view.setTemperature(d.temperature());
        switch (d.tone()) {
            case INFORMAL -> view.setTone("informal");
            case FORMAL   -> view.setTone("formal");
            default       -> view.setTone("neutral");
        }
        view.setStyle(d.grammarStyle());
        switch (d.textMode()) {
            case SUMMARIZE -> view.setTextMode("summarize");
            case EXPAND    -> view.setTextMode("expand");
            default        -> view.setTextMode("same");
        }
        if (!preset.capabilities().showTranslation()) view.setLanguage("English");
    }

    // ----------------- persistence actions -----------------
    private SessionSnapshot captureSnapshot() {
        return new SessionSnapshot(
                view.currentPresetKey(),
                safe(view.currentPresetInstruction()),
                safe(view.input.getText()),
                safe(view.output.getText()),
                view.getTemperature(),
                safe(view.getToneKey()),
                safe(view.getStyleKey()),
                safe(view.getTextModeKey()),
                safe(view.getLanguage()),
                "gpt-4o-mini",
                800,
                Instant.now()
        );
    }

    private void applySnapshotToView(SessionSnapshot s) {
        view.presetTabs.getTabs().getSelectionModel().getSelectedItem().getTabPane()
                .getTabs().stream().filter(t -> s.presetKey.equals(t.getId()))
                .findFirst().ifPresent(t -> view.presetTabs.getTabs().getSelectionModel().select(t));
        view.onPresetChanged(s.presetKey);

        view.input.setText(s.inputText);
        view.output.setText(s.outputText);
        view.setTemperature(s.temperature);
        view.setTone(s.toneKey);
        view.setStyle(s.grammarStyle);
        view.setTextMode(s.textModeKey);
        view.setLanguage(s.language);

        try {
            Method m = view.presetTabs.getClass().getMethod("setCurrentInstruction", String.class);
            m.invoke(view.presetTabs, s.instruction);
        } catch (Exception ignored) {}
    }

    private void saveCurrentRevision(String note) {
        if (currentSessionId == null) newSession();
        try {
            String revId = sessions.saveRevision(currentSessionId, captureSnapshot(), note);
            lastSession.write(currentSessionId);
            view.status.setText("Saved revision: " + revId + " (Ctrl+H to view history)");
        } catch (Exception e) {
            view.status.setText("Save failed: " + e.getMessage());
        }
    }

    private void showHistoryDialog() {
        if (currentSessionId == null) { view.status.setText("No session available."); return; }
        try {
            List<RevisionInfo> revisions = sessions.listRevisions(currentSessionId);
            if (revisions.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "No revisions yet. Use Ctrl+S to save.").showAndWait();
                return;
            }
            RevisionInfo latest = revisions.get(revisions.size() - 1);
            ChoiceDialog<RevisionInfo> chooser = new ChoiceDialog<>(latest, revisions);
            chooser.setTitle("Revision History");
            chooser.setHeaderText("Select a revision");
            chooser.setContentText("Revisions:");
            Optional<RevisionInfo> chosen = chooser.showAndWait();
            if (chosen.isEmpty()) return;

            RevisionInfo r = chosen.get();
            ButtonType loadBtn = new ButtonType("Load", ButtonBar.ButtonData.OK_DONE);
            ButtonType revertBtn = new ButtonType("Revert (delete newer)", ButtonBar.ButtonData.APPLY);
            ButtonType cancelBtn = ButtonType.CANCEL;

            Alert action = new Alert(Alert.AlertType.CONFIRMATION,
                    "What would you like to do with revision:\n" + r.revisionId,
                    loadBtn, revertBtn, cancelBtn);
            action.setTitle("Revision Action");
            Optional<ButtonType> act = action.showAndWait();
            if (act.isEmpty() || act.get() == cancelBtn) return;

            if (act.get() == loadBtn) {
                SessionSnapshot snap = sessions.loadRevision(currentSessionId, r.revisionId);
                applySnapshotToView(snap);
                view.status.setText("Loaded revision: " + r.revisionId);
            } else if (act.get() == revertBtn) {
                Alert confirm = new Alert(Alert.AlertType.WARNING,
                        "Reverting will DELETE all newer revisions.\nThis cannot be undone.",
                        ButtonType.OK, ButtonType.CANCEL);
                confirm.setTitle("Confirm Revert");
                Optional<ButtonType> res = confirm.showAndWait();
                if (res.isPresent() && res.get() == ButtonType.OK) {
                    sessions.revertToRevision(currentSessionId, r.revisionId);
                    SessionSnapshot snap = sessions.loadRevision(currentSessionId, r.revisionId);
                    applySnapshotToView(snap);
                    view.status.setText("Reverted to " + r.revisionId + ". Newer revisions deleted.");
                }
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "History error: " + e.getMessage()).showAndWait();
        }
    }

    // ----------------- export -----------------
    private void exportChooser() {
        try {
            SessionSnapshot snap = captureSnapshot();

            FileChooser fc = new FileChooser();
            fc.setTitle("Export");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Text (*.txt)", "*.txt"),
                    new FileChooser.ExtensionFilter("Markdown (*.md)", "*.md"),
                    new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf")
            );
            File f = fc.showSaveDialog(view.getRoot().getScene().getWindow());
            if (f == null) return;

            String name = f.getName().toLowerCase();
            Path out = f.toPath();

            if (name.endsWith(".txt")) {
                DocumentExporter.exportTxt(snap, out);
            } else if (name.endsWith(".md")) {
                DocumentExporter.exportMd(snap, out);
            } else if (name.endsWith(".pdf")) {
                DocumentExporter.exportPdf(snap, out);
            } else {
                // default to txt if no extension
                DocumentExporter.exportTxt(snap, out.resolveSibling(f.getName() + ".txt"));
            }
            view.status.setText("Exported to: " + out);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage()).showAndWait();
        }
    }

    // ----------------- utils -----------------
    private static String safe(String s) { return s == null ? "" : s; }
    private static String combineNonBlank(String a, String b) {
        boolean A = a != null && !a.isBlank();
        boolean B = b != null && !b.isBlank();
        if (A && B) return a + "\n" + b;
        if (A) return a;
        if (B) return b;
        return "";
    }
}