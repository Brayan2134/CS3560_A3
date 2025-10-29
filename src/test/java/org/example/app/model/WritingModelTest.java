package org.example.app.model;

import org.example.openaichatbotsdk.OpenAIChatbotSDK;
import org.example.openaichatbotsdk.QueryResult;
import org.junit.jupiter.api.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the model without real network calls by injecting a fake SDK.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WritingModelTest {

    // --- Test double (overrides network calls, records knob values) ---
    static class FakeSDK extends OpenAIChatbotSDK {
        String setModel;
        Double setTemp;
        Integer setMaxTok;
        String setTone;
        String setGrammar;
        Boolean setExpand;
        Boolean setSumm;

        QueryResult nextResult = QueryResult.complete("OK");
        String lastPrompt;
        String lastId = "resp_test_id";

        FakeSDK() {
            super("gpt-4o-mini", 0.5, 800, "neutral", "standard", false, false);
        }

        @Override public void setModel(String model) { this.setModel = model; }
        @Override public void setTemperature(double t) { this.setTemp = t; }
        @Override public void setMaxOutputTokens(int n) { this.setMaxTok = n; }
        @Override public void setTone(String tone) { this.setTone = tone; }
        @Override public void setGrammarStyle(String style) { this.setGrammar = style; }
        @Override public void setTextExpansion(boolean enabled) { this.setExpand = enabled; }
        @Override public void setTextSummarization(boolean enabled) { this.setSumm = enabled; }

        @Override public String askQuery(String prompt) {
            this.lastPrompt = prompt;
            return lastId;
        }
        @Override public QueryResult getQuery(String responseId) {
            // ignore id and return whatever was staged
            return nextResult;
        }
    }

    private String originalKey;

    @BeforeAll
    void setDummyKey() {
        originalKey = System.getProperty("openai.api.key");
        System.setProperty("openai.api.key", "test-key"); // keep parent ctor happy
    }

    @AfterAll
    void restoreKey() {
        if (originalKey == null) System.clearProperty("openai.api.key");
        else System.setProperty("openai.api.key", originalKey);
    }

    @Test
    void applyConfig_mapsKnobsCorrectly() {
        FakeSDK sdk = new FakeSDK();
        WritingModel model = new WritingModel(sdk);

        WritingConfig cfg = new WritingConfig(
                "gpt-4o-mini", 0.8, 500,
                WritingConfig.Tone.INFORMAL,
                "none", // should map to "standard"
                WritingConfig.TextMode.EXPAND
        );

        model.applyConfig(cfg);

        assertEquals("gpt-4o-mini", sdk.setModel);
        assertEquals(0.8, sdk.setTemp, 1e-9);
        assertEquals(500, sdk.setMaxTok);
        assertEquals("casual", sdk.setTone);            // INFORMAL -> "casual"
        assertEquals("standard", sdk.setGrammar);       // "none" -> "standard"
        assertEquals(Boolean.TRUE, sdk.setExpand);      // EXPAND -> true
        assertEquals(Boolean.FALSE, sdk.setSumm);       // EXPAND -> false
    }

    @Test
    void generateAsync_returnsCompleteText_andNotifiesListeners() throws Exception {
        FakeSDK sdk = new FakeSDK();
        sdk.nextResult = QueryResult.complete("Final text");
        WritingModel model = new WritingModel(sdk);

        AtomicReference<String> started = new AtomicReference<>();
        AtomicReference<String> completed = new AtomicReference<>();
        AtomicReference<String> errored = new AtomicReference<>();

        model.addListener(new WritingModelListener() {
            @Override public void onRequestStart(String prompt) { started.set(prompt); }
            @Override public void onRequestComplete(String output) { completed.set(output); }
            @Override public void onRequestError(String code, String message) { errored.set(code + ":" + message); }
        });

        String text = model.generateAsync("hello").get(2, TimeUnit.SECONDS);

        assertEquals("hello", started.get());
        assertEquals("Final text", text);
        assertEquals("Final text", completed.get());
        assertNull(errored.get());
        assertEquals("hello", sdk.lastPrompt);
    }

    @Test
    void generateAsync_returnsErrorString_andNotifiesError() throws Exception {
        FakeSDK sdk = new FakeSDK();
        sdk.nextResult = QueryResult.error("RATE_LIMIT", "Too many requests");
        WritingModel model = new WritingModel(sdk);

        AtomicReference<String> error = new AtomicReference<>();
        model.addListener(new WritingModelListener() {
            @Override public void onRequestError(String code, String message) { error.set(code + "|" + message); }
        });

        String text = model.generateAsync("prompt").get(2, TimeUnit.SECONDS);
        assertTrue(text.startsWith("[Error] RATE_LIMIT: Too many requests"));
        assertNotNull(error.get());
        assertTrue(error.get().startsWith("UNEXPECTED") || error.get().startsWith("REQUEST_FAILED"));
        // Note: model currently reports error via whenComplete by parsing the error string.
    }

    @Test
    void generateAsync_pendingReturnsMessage_andCompletes() throws Exception {
        FakeSDK sdk = new FakeSDK();
        sdk.nextResult = QueryResult.pending();
        WritingModel model = new WritingModel(sdk);

        AtomicReference<String> completed = new AtomicReference<>();
        model.addListener(new WritingModelListener() {
            @Override public void onRequestComplete(String output) { completed.set(output); }
        });

        String text = model.generateAsync("x").get(2, TimeUnit.SECONDS);
        assertTrue(text.contains("Still processing"));
        assertEquals(text, completed.get());
    }
}
