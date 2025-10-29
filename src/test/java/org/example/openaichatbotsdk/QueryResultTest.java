package org.example.openaichatbotsdk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryResultTest {

    @Test
    void pending_hasPendingStatus_andNullFields() {
        QueryResult r = QueryResult.pending();
        assertEquals(QueryResult.Status.PENDING, r.getStatus());
        assertNull(r.getText());
        assertNull(r.getErrorCode());
        assertNull(r.getMessage());
        assertNotNull(r.toString());
    }

    @Test
    void complete_hasCompleteStatus_andText() {
        QueryResult r = QueryResult.complete("hello");
        assertEquals(QueryResult.Status.COMPLETE, r.getStatus());
        assertEquals("hello", r.getText());
        assertNull(r.getErrorCode());
        assertNull(r.getMessage());
        assertNotNull(r.toString());
    }

    @Test
    void error_hasErrorStatus_andFields() {
        QueryResult r = QueryResult.error("RATE_LIMIT", "Too many requests");
        assertEquals(QueryResult.Status.ERROR, r.getStatus());
        assertNull(r.getText());
        assertEquals("RATE_LIMIT", r.getErrorCode());
        assertEquals("Too many requests", r.getMessage());
        assertNotNull(r.toString());
    }
}
