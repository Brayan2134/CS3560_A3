
# OpenAI Chatbot SDK (Java) — `openaichatbotsdk`

A minimal, self-contained Java SDK that wraps the **OpenAI Responses API**. It’s designed for course projects that need a simple, clean interface for text-generation and writing assistance.

This SDK exposes **six public methods** (four config toggles + two operations) and hides the rest of the implementation details. It supports a no-args constructor (reads API key from environment) and a full-args constructor for convenience.

---

## Features

- **Responses API**: `POST /v1/responses` and `GET /v1/responses/{id}`
- **Simple knobs**: `model`, `temperature`, `maxOutputTokens`
- **Writing toggles**: `setGrammarStyle`, `setTone`, `setTextExpansion`, `setTextSummarization`
- **Two constructors**: no-args (env) and parameterized (preconfigure all knobs)
- **Graceful errors**: 429 (rate limit), 404 (not found), 5xx, network, and unexpected errors
- **Lightweight**: Java 17 + `java.net.http` + Gson

---

## Folder Contents

Place these files under `src/main/java/org/example/openaichatbotsdk/`:

- `OpenAIChatbotSDK.java` — main SDK class
- `QueryResult.java` — simple DTO for status/text/errors

If you have a demo app (e.g., `org.example.Main`), it can live elsewhere in your project and simply import this package.

---

## Requirements

- **Java 17+**
- **Gradle 8+**
- Dependency: **Gson**

**Gradle (Kotlin DSL)**:
```kotlin
repositories { mavenCentral() }

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
tasks.test { useJUnitPlatform() }
```

---

## API Key Setup

The no-args constructor reads the API key from environment/JVM properties in this order:

1. `OPENAI_API_KEY`
2. `OPENAI_KEY` / `OPENAI_TOKEN`
3. System property `-Dopenai.api.key=...`

### Options

- **System-wide (Windows):** Control Panel → Environment Variables → add `OPENAI_API_KEY`  
  *(Restart IDE to pick up new env vars)*
- **Gradle run task forwarder:**
  ```kotlin
  tasks.withType<JavaExec>().configureEach {
      val key = System.getenv("OPENAI_API_KEY") ?: System.getProperty("openai.api.key") ?: ""
      if (key.isNotBlank()) {
          environment("OPENAI_API_KEY", key)
          systemProperty("openai.api.key", key)
      }
  }
  ```
- **One-off (PowerShell):**
  ```powershell
  $env:OPENAI_API_KEY="sk-..."
  .\gradlew run
  ```

---

## Public API

### Constructors

```java
// No-args: reads API key from env/JVM; uses safe defaults for knobs.
OpenAIChatbotSDK();

// Full-args: set knobs/toggles up front.
OpenAIChatbotSDK(String model,
                 double temperature,
                 int maxOutputTokens,
                 String tone,
                 String grammarStyle,
                 boolean textExpansion,
                 boolean textSummarization);
```

### Configuration (knobs)

```java
void setModel(String model);              // default: "gpt-4o-mini"
void setTemperature(double t);            // 0..2 (default 0.5)
void setMaxOutputTokens(int n);           // >0 (default 800)

void setGrammarStyle(String style);       // e.g., "standard", "APA"
void setTone(String tone);                // e.g., "neutral", "formal", "casual"
void setTextExpansion(boolean enabled);   // add examples / elaborate
void setTextSummarization(boolean enabled); // condense to key points
```

### Operations

```java
String askQuery(String prompt);
// -> POST /v1/responses with your instructions + input prompt
// <- returns the provider's response_id (or a temp ID on errors)

QueryResult getQuery(String responseId);
// -> GET /v1/responses/{id}
// <- PENDING / COMPLETE (with text) / ERROR (code + message)
```

### `QueryResult`

```java
public final class QueryResult {
  public enum Status { PENDING, COMPLETE, ERROR }
  Status getStatus();
  String getText();        // only when COMPLETE
  String getErrorCode();   // only when ERROR
  String getMessage();     // only when ERROR

  static QueryResult pending();
  static QueryResult complete(String text);
  static QueryResult error(String code, String message);
}
```

---

## Usage Example

```java
import org.example.openaichatbotsdk.OpenAIChatbotSDK;
import org.example.openaichatbotsdk.QueryResult;

public class Demo {
  public static void main(String[] args) {
    // No-args constructor (reads OPENAI_API_KEY)
    OpenAIChatbotSDK sdk = new OpenAIChatbotSDK();

    // Configure
    sdk.setModel("gpt-4o-mini");
    sdk.setTemperature(0.5);
    sdk.setMaxOutputTokens(600);
    sdk.setGrammarStyle("standard");
    sdk.setTone("professional");
    sdk.setTextExpansion(false);
    sdk.setTextSummarization(true);

    // Ask
    String responseId = sdk.askQuery("Summarize the key risks of migrating to a new API.");

    // Get
    QueryResult result = sdk.getQuery(responseId);
    if (result.getStatus() == QueryResult.Status.COMPLETE) {
      System.out.println(result.getText());
    } else if (result.getStatus() == QueryResult.Status.ERROR) {
      System.err.println(result.getErrorCode() + ": " + result.getMessage());
    } else {
      System.out.println("Pending... call getQuery again shortly.");
    }
  }
}
```

---

## Design Notes

- **Single entry point**: Only one class (`OpenAIChatbotSDK`) and one DTO (`QueryResult`) are public.
- **Separation of concerns**: UI layer should only call the six methods above; all HTTP details remain internal.
- **Error handling**: Maps 429 (rate limit), 404 (not found), 5xx (server), network, and unexpected exceptions to stable error codes.
- **Extensibility**: You can layer Strategy/Factory/Observer patterns **above** this SDK without changing its public surface.
- **Async option**: If needed, add `CompletableFuture` wrappers (`askQueryAsync`, `getQueryAsync`) without altering call sites.

---

## Troubleshooting

- **`OPENAI_API_KEY environment variable is not set.`**  
  Ensure the IDE/Gradle run config actually passes the env var (see “API Key Setup”).

- **401 Unauthorized**  
  Verify the API key (no stray spaces, correct project).

- **429 Rate Limit**  
  Slow down requests; backoff and retry later.

- **404 Not Found (getQuery)**  
  Ensure you’re passing the real `response_id` from `askQuery` (not a temp ID returned on earlier errors).
