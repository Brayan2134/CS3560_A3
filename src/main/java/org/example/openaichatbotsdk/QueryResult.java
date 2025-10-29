package org.example.openaichatbotsdk;

public class QueryResult {
    public enum Status { PENDING, COMPLETE, ERROR } //Lifecycle states for a model response.

    private final Status status;
    private final String text;
    private final String errorCode;
    private final String message;

    private QueryResult(Status status, String text, String errorCode, String message) {
        this.status = status;
        this.text = text;
        this.errorCode = errorCode;
        this.message = message;
    }

    // ---------- Factory methods ----------

    /**
     * Create a pending result.
     * <p>Why: Represent in-progress work before content is available.</p>
     * @return A {@code QueryResult} with status PENDING.
     * @pre none
     * @post Result has status PENDING; text/error fields null.
     */
    public static QueryResult pending() {
        return new QueryResult(Status.PENDING, null, null, null);
    }

    /**
     * Create a complete result with text.
     * <p>Why: Represent a successful, finished response.</p>
     * @param text Non-null assistant text content.
     * @return A {@code QueryResult} with status COMPLETE.
     * @pre {@code text != null}.
     * @post Result has status COMPLETE; {@code getText()} equals {@code text}.
     */
    public static QueryResult complete(String text) {
        return new QueryResult(Status.COMPLETE, text, null, null);
    }

    /**
     * Create an error result.
     * <p>Why: Represent a failure/exceptional state with a stable code and message.</p>
     * @param code Stable error code (e.g., RATE_LIMIT, NOT_FOUND, NETWORK_ERROR).
     * @param message Human-readable description of the error.
     * @return A {@code QueryResult} with status ERROR.
     * @pre {@code code != null}; {@code message != null}.
     * @post Result has status ERROR; {@code getErrorCode()==code}, {@code getMessage()==message}.
     */
    public static QueryResult error(String code, String message) {
        return new QueryResult(Status.ERROR, null, code, message);
    }

    /**
     * @return The current state of the response lifecycle.
     * @pre none
     * @post Returned value is one of PENDING, COMPLETE, or ERROR.
     */
    public Status getStatus() { return status; }

    /**
     * @return The assistant text when status == COMPLETE; otherwise null.
     * @pre none
     * @post Non-null only if {@link #getStatus()} == COMPLETE.
     */
    public String getText() { return text; }

    /**
     * @return A stable error code when status == ERROR (e.g., RATE_LIMIT, NOT_FOUND, HTTP_500); else null.
     * @pre none
     * @post Non-null only if {@link #getStatus()} == ERROR.
     */
    public String getErrorCode() { return errorCode; }

    /**
     * @return A human-readable error message when status == ERROR; else null.
     * @pre none
     * @post Non-null only if {@link #getStatus()} == ERROR.
     */
    public String getMessage() { return message; }

    /**
     * @return Debug-friendly summary containing status and truncated text/message.
     * @pre none
     * @post Returns a non-null String.
     */
    @Override
    public String toString() {
        return "QueryResult{" +
                "status=" + status +
                ", text=" + (text == null ? "null" : (text.length() > 120 ? text.substring(0, 120) + "..." : text)) +
                ", errorCode='" + errorCode + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
