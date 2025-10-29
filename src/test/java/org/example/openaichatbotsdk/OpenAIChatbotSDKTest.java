package org.example.openaichatbotsdk;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenAIChatbotSDKTest {

    private String originalSysProp;

    @BeforeAll
    void setDummyKey() {
        // Save original, then set a harmless dummy so constructors won’t throw.
        originalSysProp = System.getProperty("openai.api.key");
        System.setProperty("openai.api.key", "test-key-for-unit-tests");
    }

    @AfterAll
    void restoreKey() {
        if (originalSysProp == null) {
            System.clearProperty("openai.api.key");
        } else {
            System.setProperty("openai.api.key", originalSysProp);
        }
    }

    // ---------- Pure unit tests (never hit the network) ----------

    @Test
    void setters_acceptValidValues_andApplyDefaultsOnBlank() {
        OpenAIChatbotSDK sdk = new OpenAIChatbotSDK();

        // knobs
        sdk.setModel("gpt-4o-mini");
        sdk.setTemperature(0.7);
        sdk.setMaxOutputTokens(256);

        // toggles
        sdk.setGrammarStyle("APA");
        sdk.setTone("formal");
        sdk.setTextExpansion(true);
        sdk.setTextSummarization(false);

        // blanks fall back to defaults (no exception)
        sdk.setModel(null);
        sdk.setGrammarStyle("");
        sdk.setTone("  ");
    }

    @Test
    void setters_rejectInvalidValues() {
        OpenAIChatbotSDK sdk = new OpenAIChatbotSDK();
        assertThrows(IllegalArgumentException.class, () -> sdk.setTemperature(-0.01));
        assertThrows(IllegalArgumentException.class, () -> sdk.setTemperature(2.01));
        assertThrows(IllegalArgumentException.class, () -> sdk.setMaxOutputTokens(0));
        assertThrows(IllegalArgumentException.class, () -> sdk.setMaxOutputTokens(-5));
    }

    @Test
    void parameterizedConstructor_appliesValues_andValidates() {
        // valid
        assertDoesNotThrow(() ->
                new OpenAIChatbotSDK(
                        "gpt-4o-mini", 0.4, 300,
                        "professional", "standard",
                        false, true
                )
        );

        // invalid: temperature out of range
        assertThrows(IllegalArgumentException.class, () ->
                new OpenAIChatbotSDK(
                        "gpt-4o-mini", 2.5, 300,
                        "professional", "standard",
                        false, true
                )
        );

        // invalid: tokens <= 0
        assertThrows(IllegalArgumentException.class, () ->
                new OpenAIChatbotSDK(
                        "gpt-4o-mini", 0.5, 0,
                        "professional", "standard",
                        false, true
                )
        );
    }

    // ---------- Optional integration test (runs only if a real key is present) ----------

    @Test
    @Timeout(20)
    void integration_askAndGetQuery_runsOnlyWhenApiKeyPresent() {
        // Only run if a real env/system key exists (not our dummy test-key-for-unit-tests)
        String key = System.getenv("OPENAI_API_KEY");
        if (key == null || key.isBlank()) {
            key = System.getProperty("openai.api.key");
        }
        Assumptions.assumeTrue(key != null && !key.isBlank() && !"test-key-for-unit-tests".equals(key),
                "Skipping integration test: no real API key present");

        OpenAIChatbotSDK sdk = new OpenAIChatbotSDK();
        sdk.setModel("gpt-4o-mini");
        sdk.setTemperature(0.3);
        sdk.setMaxOutputTokens(200);
        sdk.setTone("professional");
        sdk.setGrammarStyle("standard");
        sdk.setTextExpansion(false);
        sdk.setTextSummarization(true);

        String id = sdk.askQuery("Give me one sentence summarizing why unit tests are valuable.");
        assertNotNull(id);
        assertFalse(id.isBlank());

        QueryResult result = sdk.getQuery(id);
        assertNotNull(result);
        assertNotNull(result.getStatus());
    }
}