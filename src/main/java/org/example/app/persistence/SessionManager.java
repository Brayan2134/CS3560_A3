package org.example.app.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.Instant;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

/**
 * File-based session store with immutable revisions and manifest.
 * Thread-safety: methods synchronized for simplicity (single-user desktop).
 */
public class SessionManager {
    private final Path root; // e.g., Paths.get(System.getProperty("user.home"), ".writing-assistant", "sessions")
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss'Z'").withZone(java.time.ZoneOffset.UTC);

    public SessionManager(Path rootDirectory) {
        this.root = rootDirectory;
    }

    // ---------- Session lifecycle ----------

    private static final JsonSerializer<Instant> INSTANT_SER = new JsonSerializer<>() {
        @Override public JsonElement serialize(Instant src, Type t, JsonSerializationContext c) {
            return new JsonPrimitive(src.toString()); // ISO-8601
        }
    };
    private static final JsonDeserializer<Instant> INSTANT_DESER = new JsonDeserializer<>() {
        @Override public Instant deserialize(JsonElement json, Type t, JsonDeserializationContext c)
                throws JsonParseException {
            return Instant.parse(json.getAsString());
        }
    };

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, INSTANT_SER)
            .registerTypeAdapter(Instant.class, INSTANT_DESER)
            .setPrettyPrinting()
            .create();

    public synchronized String createSession(String title) throws IOException {
        String sessionId = UUID.randomUUID().toString();
        Path dir = sessionDir(sessionId);
        Files.createDirectories(dir.resolve("revisions"));
        SessionInfo info = new SessionInfo(sessionId, title, Instant.now());
        writeManifest(info);
        return sessionId;
    }

    private SessionInfo tryReadManifest(Path manifest) {
        try (Reader r = Files.newBufferedReader(manifest, StandardCharsets.UTF_8)) {
            return gson.fromJson(r, SessionInfo.class);
        } catch (Exception e) {
            // log if you want; skipping bad manifest keeps UI stable
            return null;
        }
    }

    public synchronized List<SessionInfo> listSessions() throws IOException {
        if (!Files.exists(root)) return List.of();
        try (var dirs = Files.list(root)) {
            List<SessionInfo> out = new ArrayList<>();
            for (Path sDir : dirs.filter(Files::isDirectory).toList()) {
                Path manifest = sDir.resolve("manifest.json");
                if (!Files.exists(manifest)) continue;
                SessionInfo si = tryReadManifest(manifest);
                if (si == null) continue;               // skip corrupt/partial
                if (si.updatedAt == null) si.updatedAt = si.createdAt; // coalesce
                out.add(si);
            }
            // newest first; guard updatedAt
            out.sort(Comparator.comparing(
                    (SessionInfo si) -> si.updatedAt == null ? si.createdAt : si.updatedAt
            ).reversed());
            return out;
        }
    }

    public synchronized void deleteSession(String sessionId) throws IOException {
        Path dir = sessionDir(sessionId);
        if (Files.exists(dir)) {
            try (var walk = Files.walk(dir)) {
                List<Path> paths = walk.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
                for (Path p : paths) Files.deleteIfExists(p);
            }
        }
    }

    // ---------- Revisions ----------

    /** Save a new revision (snapshot). Returns the revisionId (timestamp). */
    public synchronized String saveRevision(String sessionId, SessionSnapshot snap, String note) throws IOException {
        SessionInfo info = readManifest(sessionId);
        String revId = TS.format(Instant.now());
        Path revFile = revisionFile(sessionId, revId);
        try (Writer w = Files.newBufferedWriter(revFile)) {
            gson.toJson(snap, w);
        }
        info.revisions.add(new RevisionInfo(revId, Instant.now(), note));
        info.updatedAt = Instant.now();
        writeManifest(info);
        return revId;
    }

    public synchronized List<RevisionInfo> listRevisions(String sessionId) throws IOException {
        return new ArrayList<>(readManifest(sessionId).revisions);
    }

    public synchronized SessionSnapshot loadRevision(String sessionId, String revisionId) throws IOException {
        Path revFile = revisionFile(sessionId, revisionId);
        try (Reader r = Files.newBufferedReader(revFile)) {
            return gson.fromJson(r, SessionSnapshot.class);
        }
    }

    /** Revert manifest to the given revision, deleting later revisions' files. */
    public synchronized void revertToRevision(String sessionId, String revisionId) throws IOException {
        SessionInfo info = readManifest(sessionId);

        int idx = -1;
        for (int i = 0; i < info.revisions.size(); i++) {
            if (info.revisions.get(i).revisionId.equals(revisionId)) { idx = i; break; }
        }
        if (idx < 0) throw new IllegalArgumentException("Revision not found: " + revisionId);

        // delete files after idx
        for (int i = info.revisions.size() - 1; i > idx; i--) {
            String rid = info.revisions.get(i).revisionId;
            Files.deleteIfExists(revisionFile(sessionId, rid));
            info.revisions.remove(i);
        }
        info.updatedAt = Instant.now();
        writeManifest(info);
    }

    public synchronized SessionSnapshot loadLatest(String sessionId) throws IOException {
        SessionInfo info = readManifest(sessionId);
        if (info.revisions.isEmpty()) return null;
        String rid = info.revisions.get(info.revisions.size() - 1).revisionId;
        return loadRevision(sessionId, rid);
    }

    // ---------- Helpers ----------

    private Path sessionDir(String sessionId) {
        return root.resolve(sessionId);
    }
    private Path manifestPath(String sessionId) {
        return sessionDir(sessionId).resolve("manifest.json");
    }
    private Path revisionFile(String sessionId, String revisionId) {
        return sessionDir(sessionId).resolve("revisions").resolve(revisionId + ".json");
    }

    private SessionInfo readManifest(String sessionId) throws IOException {
        return readManifest(manifestPath(sessionId));
    }
    private SessionInfo readManifest(Path manifest) throws IOException {
        try (Reader r = Files.newBufferedReader(manifest, StandardCharsets.UTF_8)) {
            return gson.fromJson(r, SessionInfo.class);
        } catch (Exception e) {
            throw new IOException("Bad manifest: " + manifest.getFileName(), e);
        }
    }
    private void writeManifest(SessionInfo info) throws IOException {
        Files.createDirectories(sessionDir(info.sessionId));
        try (Writer w = Files.newBufferedWriter(manifestPath(info.sessionId), StandardCharsets.UTF_8)) {
            gson.toJson(info, w);
        }
    }
}