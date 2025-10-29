package org.example.openaichatbotsdk;

import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Minimal SDK wrapper around OpenAI's Responses API.
 *
 * Public API (6 methods you asked for):
 *  - setGrammarStyle, setTone, setTextExpansion, setTextSummarization
 *  - askQuery(prompt)  -> returns responseId
 *  - getQuery(id)      -> QueryResult (PENDING / COMPLETE / ERROR)
 *
 * Knobs (simple, readable):
 *  - model, temperature (0..2), maxOutputTokens (>0)
 *
 * Constructors:
 *  - OpenAIChatbotSDK()                            // no-args, reads OPENAI_API_KEY (with fallbacks)
 *  - OpenAIChatbotSDK(String model, double temperature, int maxOutputTokens,
 *                     String tone, String grammarStyle,
 *                     boolean textExpansion, boolean textSummarization)
 */
public class OpenAIChatbotSDK {

    // ===== Constants =====
    private static final URI RESPONSES_URI = URI.create("https://api.openai.com/v1/responses");

    // ===== HTTP + JSON =====
    private final String apiKey;
    private final HttpClient http;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    // ===== Knobs (simple + obvious) =====
    private String model = "gpt-4o-mini";
    private double temperature = 0.5;     // 0..2
    private int maxOutputTokens = 800;    // >0

    // ===== Writing behavior toggles =====
    private String grammarStyle = "standard"; // "standard", "APA", "Chicago", etc.
    private String tone = "neutral";          // "neutral", "formal", "casual", "professional", etc.
    private boolean textExpansion = false;
    private boolean textSummarization = false;

    // Local cache of results (id -> QueryResult)
    private final Map<String, QueryResult> cache = new LinkedHashMap<>();

    // ===== Constructors =====

    /**
     * No-args constructor.
     * <p>Purpose: Creates an SDK instance using environment/JVM properties for the OpenAI API key
     * and default model/temperature/token settings.</p>
     * @pre {@code OPENAI_API_KEY} (or accepted fallback key) is set and non-blank.
     * @post Instance is initialized; subsequent calls may configure knobs and invoke API methods.
     */
    public OpenAIChatbotSDK() {
        this.apiKey = requireApiKey(resolveApiKey());
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
    }

    /**
     * Full-args constructor.
     * <p>Purpose: Create an SDK instance with all knobs/toggles set up front
     * while still reading the API key from env/JVM properties.</p>
     * @param model            Model name (e.g., "gpt-4o-mini").
     * @param temperature      Sampling temperature in [0, 2].
     * @param maxOutputTokens  Maximum output tokens (> 0).
     * @param tone             Desired tone (e.g., "neutral", "formal").
     * @param grammarStyle     Grammar/style guide label (e.g., "standard", "APA").
     * @param textExpansion    Whether to expand ideas.
     * @param textSummarization Whether to summarize output.
     * @pre API key available via environment/JVM; {@code temperature∈[0,2]}, {@code maxOutputTokens>0}.
     * @post Instance configured with provided values; ready to call {@link #askQuery(String)}.
     */
    public OpenAIChatbotSDK(String model,
                            double temperature,
                            int maxOutputTokens,
                            String tone,
                            String grammarStyle,
                            boolean textExpansion,
                            boolean textSummarization) {
        this.apiKey = requireApiKey(resolveApiKey());
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        setModel(model);
        setTemperature(temperature);
        setMaxOutputTokens(maxOutputTokens);
        setTone(tone);
        setGrammarStyle(grammarStyle);
        setTextExpansion(textExpansion);
        setTextSummarization(textSummarization);
    }

    // ===== Public Config (easy knobs) =====

    /**
     * Set the target OpenAI model.
     * <p>Why: Lets callers choose capability/cost/perf trade-off without changing code elsewhere.</p>
     * @param model Model name; if null/blank, a sane default is used.
     * @pre none
     * @post Subsequent requests use this model unless changed again.
     */
    public void setModel(String model) {
        this.model = (model == null || model.isBlank()) ? "gpt-4o-mini" : model.trim();
    }

    /**
     * Set sampling temperature.
     * <p>Why: Controls creativity vs. determinism.</p>
     * @param t Temperature in [0, 2].
     * @pre {@code 0 <= t <= 2}
     * @post Subsequent requests use this temperature.
     */
    public void setTemperature(double t) {
        if (t < 0.0 || t > 2.0) {
            throw new IllegalArgumentException("temperature must be in [0, 2]");
        }
        this.temperature = t;
    }

    /**
     * Set the max number of output tokens.
     * <p>Why: Bounds response length/cost.</p>
     * @param n Positive integer token cap.
     * @pre {@code n > 0}
     * @post Subsequent requests use this token cap.
     */
    public void setMaxOutputTokens(int n) {
        if (n <= 0) throw new IllegalArgumentException("maxOutputTokens must be > 0");
        this.maxOutputTokens = n;
    }

    /**
     * Set grammar/style guideline.
     * <p>Why: Nudges the assistant toward a style family.</p>
     * @param style Style label (e.g., "standard", "APA"); null/blank resets to default.
     * @pre none
     * @post Affects instruction text for future requests.
     */
    public void setGrammarStyle(String style) {
        this.grammarStyle = (style == null || style.isBlank()) ? "standard" : style.trim();
    }


    /**
     * Set tone of voice.
     * <p>Why: Quickly switch between neutral/formal/casual/etc.</p>
     * @param tone Tone label; null/blank resets to default.
     * @pre none
     * @post Affects instruction text for future requests.
     */
    public void setTone(String tone) {
        this.tone = (tone == null || tone.isBlank()) ? "neutral" : tone.trim();
    }

    /**
     * Enable/disable text expansion.
     * <p>Why: Ask the model to elaborate on ideas.</p>
     * @param enabled True to expand, false otherwise.
     * @pre none
     * @post Future requests include expansion guidance when enabled.
     */
    public void setTextExpansion(boolean enabled) {
        this.textExpansion = enabled;
    }

    /**
     * Enable/disable text summarization.
     * <p>Why: Ask the model to condense to key points.</p>
     * @param enabled True to summarize, false otherwise.
     * @pre none
     * @post Future requests include summarization guidance when enabled.
     */
    public void setTextSummarization(boolean enabled) {
        this.textSummarization = enabled;
    }

    // ===== Public Ops =====

    /**
     * Submit a prompt to the OpenAI Responses API.
     * <p>Why: Starts a model response and returns its {@code response_id} for later retrieval.</p>
     * @param prompt User text to process.
     * @return The provider-assigned response ID (string) or a locally generated ID for error cases.
     * @pre API key configured; {@code prompt != null}.
     * @post A request is sent; a local cache entry is created with PENDING/COMPLETE/ERROR status.
     */
    public String askQuery(String prompt) {
        Objects.requireNonNull(prompt, "prompt");

        System.out.println("requesting model response...");

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("instructions", buildInstructions());
        body.addProperty("temperature", temperature);
        body.addProperty("max_output_tokens", maxOutputTokens);
        body.addProperty("store", true); // keep retrievable
        body.add("input", new JsonPrimitive(prompt)); // text input per Responses API

        HttpRequest req = HttpRequest.newBuilder(RESPONSES_URI)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body), StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (res.statusCode() == 200) {
                JsonObject root = JsonParser.parseString(res.body()).getAsJsonObject();
                String responseId = getAsString(root, "id");
                String status = getAsString(root, "status");
                String text = extractOutputText(root);

                QueryResult qr = "completed".equalsIgnoreCase(status)
                        ? QueryResult.complete(text)
                        : QueryResult.pending();

                cache.put(responseId, qr);

                System.out.println("received model response.");
                return responseId;

            } else if (res.statusCode() == 429) {
                String id = UUID.randomUUID().toString();
                cache.put(id, QueryResult.error("RATE_LIMIT", "Rate limit exceeded. Try again shortly."));
                System.err.println("error: rate limit exceeded (429).");
                return id;

            } else {
                String id = UUID.randomUUID().toString();
                cache.put(id, QueryResult.error("HTTP_" + res.statusCode(), extractErrorMessage(res.body())));
                System.err.println("error: unexpected HTTP status " + res.statusCode() + ".");
                return id;
            }

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            String id = UUID.randomUUID().toString();
            cache.put(id, QueryResult.error("NETWORK_ERROR", e.getMessage()));
            System.err.println("error: network/timeout while requesting model response.");
            return id;

        } catch (Exception e) {
            String id = UUID.randomUUID().toString();
            cache.put(id, QueryResult.error("UNEXPECTED", e.toString()));
            System.err.println("error: unexpected exception while requesting model response.");
            return id;
        }
    }

    /**
     * Retrieve the latest status and content for a given response ID.
     * <p>Why: Poll or fetch completed content using the ID from {@link #askQuery(String)}.</p>
     * @param responseId The id returned by {@code askQuery}.
     * @return A {@link QueryResult} representing PENDING, COMPLETE (with text), or ERROR (with code/message).
     * @pre {@code responseId} not null/blank.
     * @post If remote status is 200/complete, local cache updated to COMPLETE; else returns current state.
     */
    public QueryResult getQuery(String responseId) {
        if (responseId == null || responseId.isBlank()) {
            return QueryResult.error("BAD_REQUEST", "responseId is required");
        }

        QueryResult cached = cache.get(responseId);
        if (cached != null && cached.getStatus() != QueryResult.Status.PENDING) {
            return cached;
        }

        URI getUri = URI.create(RESPONSES_URI.toString() + "/" + responseId);
        HttpRequest req = HttpRequest.newBuilder(getUri)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (res.statusCode() == 200) {
                JsonObject root = JsonParser.parseString(res.body()).getAsJsonObject();
                String status = getAsString(root, "status");

                if ("completed".equalsIgnoreCase(status)) {
                    String text = extractOutputText(root);
                    QueryResult qr = QueryResult.complete(text);
                    cache.put(responseId, qr);
                    return qr;
                } else {
                    QueryResult qr = QueryResult.pending();
                    cache.put(responseId, qr);
                    return qr;
                }

            } else if (res.statusCode() == 404) {
                QueryResult qr = QueryResult.error("NOT_FOUND", "Response not found: " + responseId);
                cache.put(responseId, qr);
                System.err.println("error: response not found (404).");
                return qr;

            } else if (res.statusCode() == 429) {
                System.err.println("error: rate limit exceeded (429) during getQuery.");
                return QueryResult.error("RATE_LIMIT", "Rate limit exceeded. Try again shortly.");

            } else {
                System.err.println("error: unexpected HTTP status " + res.statusCode() + " during getQuery.");
                return QueryResult.error("HTTP_" + res.statusCode(), extractErrorMessage(res.body()));
            }

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            System.err.println("error: network/timeout during getQuery.");
            return QueryResult.error("NETWORK_ERROR", e.getMessage());
        } catch (Exception e) {
            System.err.println("error: unexpected exception during getQuery.");
            return QueryResult.error("UNEXPECTED", e.toString());
        }
    }

    // ===== Private helpers =====

    private String buildInstructions() {
        // Small, clear instruction string influenced by your toggles.
        List<String> lines = new ArrayList<>();
        lines.add("You are a careful writing assistant.");
        lines.add("Tone: " + tone + ".");
        lines.add("Grammar/style guide: " + grammarStyle + ".");
        if (textExpansion && !textSummarization) {
            lines.add("Expand ideas with concrete examples and improved flow.");
        } else if (textSummarization && !textExpansion) {
            lines.add("Return a concise summary capturing key points.");
        } else if (textExpansion && textSummarization) {
            lines.add("Improve clarity first, then provide BOTH an expanded version and a short summary labeled clearly.");
        } else {
            lines.add("Focus on clarity, correctness, and coherence.");
        }
        lines.add("Avoid inventing facts; briefly state assumptions if needed.");
        return String.join(" ", lines);
    }

    /** Extracts assistant text from the Responses API object. */
    private String extractOutputText(JsonObject root) {
        StringBuilder sb = new StringBuilder();
        try {
            JsonArray output = root.getAsJsonArray("output");
            if (output == null) return "(no content)";
            for (JsonElement outEl : output) {
                JsonObject outObj = outEl.getAsJsonObject();
                if (!"message".equals(getAsString(outObj, "type"))) continue;
                JsonArray content = outObj.getAsJsonArray("content");
                if (content == null) continue;
                for (JsonElement cEl : content) {
                    JsonObject cObj = cEl.getAsJsonObject();
                    if ("output_text".equals(getAsString(cObj, "type"))) {
                        String text = getAsString(cObj, "text");
                        if (text != null) {
                            if (sb.length() > 0) sb.append("\n");
                            sb.append(text);
                        }
                    }
                }
            }
        } catch (Exception ignore) { }
        return sb.length() == 0 ? "(no content)" : sb.toString();
    }

    private String extractErrorMessage(String body) {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            if (root.has("error") && root.get("error").isJsonObject()) {
                JsonObject err = root.getAsJsonObject("error");
                if (err.has("message")) return err.get("message").getAsString();
            }
        } catch (Exception ignore) { }
        return "Unexpected response.";
    }

    private static String getAsString(JsonObject obj, String key) {
        if (obj == null || key == null || !obj.has(key) || obj.get(key).isJsonNull()) return null;
        try { return obj.get(key).getAsString(); } catch (Exception e) { return null; }
    }

    private static String resolveApiKey() {
        // Preferred
        String k = System.getenv("OPENAI_API_KEY");
        if (k != null && !k.isBlank()) return k;
        // Common alternates
        for (String alt : new String[]{"OPENAI_KEY", "OPENAI_TOKEN"}) {
            k = System.getenv(alt);
            if (k != null && !k.isBlank()) return k;
        }
        // JVM property: -Dopenai.api.key=sk-...
        k = System.getProperty("openai.api.key");
        return (k == null || k.isBlank()) ? null : k;
    }

    private static String requireApiKey(String key) {
        if (key == null) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY not set. Set an env var (OPENAI_API_KEY) or pass -Dopenai.api.key=..."
            );
        }
        return key;
    }
}