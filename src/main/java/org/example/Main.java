package org.example;

import org.example.openaichatbotsdk.OpenAIChatbotSDK;
import org.example.openaichatbotsdk.QueryResult;

public class Main {
    public static void main(String[] args) {
        try {
            OpenAIChatbotSDK sdk = new OpenAIChatbotSDK();

            sdk.setGrammarStyle("standard");
            sdk.setTone("professional");
            sdk.setTextExpansion(false);
            sdk.setTextSummarization(true);

            String responseId = sdk.askQuery(
                    "Write a two-sentence status update about migrating to a new API, including one risk and one next step."
            );

            QueryResult result = sdk.getQuery(responseId);
            System.out.println("Response ID: " + responseId);
            System.out.println("Status: " + result.getStatus());
            if (result.getStatus() == QueryResult.Status.COMPLETE) {
                System.out.println("\n--- OUTPUT ---\n" + result.getText());
            } else if (result.getStatus() == QueryResult.Status.ERROR) {
                System.out.println("Error: " + result.getErrorCode() + " - " + result.getMessage());
            } else {
                System.out.println("(Pending — call getQuery again in a moment)");
            }
        } catch (Throwable t) {
            System.err.println("FATAL: " + t);
            t.printStackTrace();
            // Do NOT rethrow—prevents Gradle from failing the exec with exit code 1.
        }
    }
}