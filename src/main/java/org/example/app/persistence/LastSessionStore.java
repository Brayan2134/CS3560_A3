package org.example.app.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Stores/loads the last opened session id. */
public class LastSessionStore {
    private static final class Holder { String sessionId; }
    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public LastSessionStore(Path file) { this.file = file; }

    public String read() {
        try {
            if (!Files.exists(file)) return null;
            try (Reader r = Files.newBufferedReader(file)) {
                Holder h = gson.fromJson(r, Holder.class);
                return h == null ? null : h.sessionId;
            }
        } catch (Exception e) { return null; }
    }

    public void write(String sessionId) {
        try {
            Files.createDirectories(file.getParent());
            Holder h = new Holder(); h.sessionId = sessionId;
            try (Writer w = Files.newBufferedWriter(file)) { gson.toJson(h, w); }
        } catch (Exception ignored) {}
    }
}