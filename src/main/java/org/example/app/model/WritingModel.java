package org.example.app.model;

import org.example.openaichatbotsdk.OpenAIChatbotSDK;
import org.example.openaichatbotsdk.QueryResult;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Owns the SDK, applies config, and exposes async generation methods.
 * This class is UI-agnostic and safe to unit test.
 */
public class WritingModel {

    private final OpenAIChatbotSDK sdk;
    private final List<WritingModelListener> listeners = new CopyOnWriteArrayList<>();

    private WritingConfig activeConfig = WritingConfig.defaults();

    public WritingModel() {
        this.sdk = new OpenAIChatbotSDK(); // uses OPENAI_API_KEY from env/JVM
        // apply defaults once
        applyConfig(activeConfig);
    }

    /** Allow DI in tests if you want to inject a fake SDK later. */
    public WritingModel(OpenAIChatbotSDK sdk) {
        this.sdk = Objects.requireNonNull(sdk, "sdk");
        applyConfig(activeConfig);
    }

    // ---------- Observer wiring ----------
    public void addListener(WritingModelListener l)    { if (l != null) listeners.add(l); }
    public void removeListener(WritingModelListener l) { listeners.remove(l); }

    // ---------- Configuration ----------
    public void applyConfig(WritingConfig cfg) {
        Objects.requireNonNull(cfg, "cfg");
        this.activeConfig = cfg;

        // Map config → SDK knobs
        sdk.setModel(cfg.model());
        sdk.setTemperature(cfg.temperature());
        sdk.setMaxOutputTokens(cfg.maxOutputTokens());

        // tone mapping
        switch (cfg.tone()) {
            case INFORMAL -> sdk.setTone("casual");
            case FORMAL   -> sdk.setTone("formal");
            default       -> sdk.setTone("neutral");
        }

        // grammarStyle mapping
        if ("none".equalsIgnoreCase(cfg.grammarStyle())) {
            sdk.setGrammarStyle("standard");
        } else {
            sdk.setGrammarStyle(cfg.grammarStyle());
        }

        // expansion/summarization toggles
        sdk.setTextExpansion(cfg.expand());
        sdk.setTextSummarization(cfg.summarize());
    }

    public WritingConfig getActiveConfig() {
        return activeConfig;
    }

    // ---------- Operations ----------
    /**
     * Fire-and-forget async call that returns the final text (or an error string).
     * Controller can show a spinner while this runs.
     */
    public CompletableFuture<String> generateAsync(String userText) {
        Objects.requireNonNull(userText, "userText");
        listeners.forEach(l -> l.onRequestStart(userText));

        // Use your SDK synchronously under the hood; wrap in CompletableFuture
        return CompletableFuture.supplyAsync(() -> {
            String id = sdk.askQuery(userText);
            QueryResult qr = sdk.getQuery(id);
            return switch (qr.getStatus()) {
                case COMPLETE -> qr.getText();
                case ERROR    -> "[Error] " + qr.getErrorCode() + ": " + qr.getMessage();
                case PENDING  -> "Still processing... try again in a moment.";
            };
        }).whenComplete((text, err) -> {
            if (err != null) {
                listeners.forEach(l -> l.onRequestError("UNEXPECTED", err.getMessage()));
            } else if (text != null && text.startsWith("[Error] ")) {
                // crude parse to notify observers with structured error if needed
                listeners.forEach(l -> l.onRequestError("REQUEST_FAILED", text));
            } else {
                listeners.forEach(l -> l.onRequestComplete(text));
            }
        });
    }
}