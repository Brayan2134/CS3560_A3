package org.example.app.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LastSessionStoreTest {

    @Test
    void writeThenRead_roundTrip(@TempDir Path tmp) {
        Path f = tmp.resolve("state").resolve("last.json");
        LastSessionStore store = new LastSessionStore(f);

        assertNull(store.read(), "no file yet → null");

        store.write("abc-123");
        assertTrue(Files.exists(f));
        assertEquals("abc-123", store.read());

        // overwrite with another id
        store.write("xyz-789");
        assertEquals("xyz-789", store.read());
    }

    @Test
    void read_handlesCorruptionGracefully(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("state").resolve("last.json");
        Files.createDirectories(f.getParent());
        Files.writeString(f, "{not valid json");

        LastSessionStore store = new LastSessionStore(f);
        assertNull(store.read(), "corrupt file → null (non-throwing)");
    }
}