package org.example.app.persistence;

import com.google.gson.*;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * File-based session store with immutable revisions kept in a manifest.
 *
 * Layout:
 *   <root>/<sessionId>/manifest.json
 *   <root>/<sessionId>/revisions/<revisionId>.json
 *
 * Manifest JSON:
 *   {
 *     "sessionId": "...",
 *     "title": "...",
 *     "createdAt": "ISO-8601",
 *     "updatedAt": "ISO-8601",
 *     "revisions": [
 *       {"revisionId":"...","createdAt":"ISO-8601","note":"..."},
 *       ...
 *     ]
 *   }
 *
 * Thread-safety: all public methods are synchronized (single-user desktop app).
 */
public final class SessionManager {

    private final Path root;
    private final Gson gson;

    /**
     * Non-throwing constructor (per controller contract).
     *
     * Does: Ensures {@code rootDirectory} exists; prepares Gson with Instant adapter.
     * Preconditions: {@code rootDirectory} non-null.
     * Postconditions: Root exists; throws UncheckedIOException if it can’t be created.
     * Notes: Keeps UI code free of try/catch at construction time.
     */
    public SessionManager(Path rootDirectory) {
        this.root = Objects.requireNonNull(rootDirectory, "rootDirectory");
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            // Keep controller simple: fail fast as unchecked if the FS is unusable
            throw new UncheckedIOException("Failed to create sessions root: " + this.root, e);
        }
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantAdapter())
                .setPrettyPrinting()
                .create();
    }

    // ---------- Sessions ----------

    /**
     * Creates a new session directory with an empty manifest.
     *
     * Does: Makes <root>/<sessionId>/revisions and writes manifest.json.
     * Preconditions: None; {@code title} may be null/blank.
     * Postconditions: Returns generated {@code sessionId}; session exists on disk.
     * Throws: IOException on filesystem failure.
     * Notes: Sets updatedAt = createdAt so sorting works immediately.
     */
    public synchronized String createSession(String title) throws IOException {
        String sid = UUID.randomUUID().toString();
        Path dir = sessionDir(sid);
        Files.createDirectories(dir.resolve("revisions"));

        SessionInfo info = new SessionInfo(sid, title == null ? "" : title, Instant.now());
        info.updatedAt = info.createdAt; // non-null so sorting by updatedAt works immediately
        writeManifest(dir, info, new ArrayList<>());
        return sid;
    }

    /**
     * Lists all sessions, newest-first.
     *
     * Does: Reads each manifest header; sorts by updatedAt (fallback createdAt) DESC.
     * Preconditions: Root may be empty; corrupt manifests are skipped.
     * Postconditions: Returns an immutable snapshot of metadata.
     * Throws: IOException on directory listing or header read failure.
     */
    public synchronized List<SessionInfo> listSessions() throws IOException {
        if (!Files.exists(root)) return List.of();

        try (var dirs = Files.list(root)) {
            List<Path> sessionDirs = dirs.filter(Files::isDirectory).collect(Collectors.toList());
            List<SessionInfo> out = new ArrayList<>(sessionDirs.size());

            for (Path sDir : sessionDirs) {
                Path manifest = sDir.resolve("manifest.json");
                if (!Files.exists(manifest)) continue;
                SessionInfo si = tryReadManifestHeader(manifest);
                if (si != null) out.add(si); // skip corrupt
            }

            out.sort((a, b) -> {
                Instant at = a.updatedAt != null ? a.updatedAt : a.createdAt;
                Instant bt = b.updatedAt != null ? b.updatedAt : b.createdAt;
                return bt.compareTo(at); // DESC newest-first
            });
            return out;
        }
    }

    /**
     * Deletes a session directory tree.
     *
     * Does: Recursively removes <root>/<sessionId>.
     * Preconditions: {@code sessionId} exists or not; both are OK.
     * Postconditions: Session folder is gone (best-effort).
     * Throws: IOException if deletion fails.
     */
    public synchronized void deleteSession(String sessionId) throws IOException {
        Path dir = sessionDir(sessionId);
        if (!Files.exists(dir)) return;
        try (var walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                Files.deleteIfExists(p);
            }
        }
    }

    // ---------- Revisions ----------

    /**
     * Saves a snapshot as a new revision and appends it to the manifest.
     *
     * Does: Writes revisions/<revisionId>.json and updates manifest (chronological append).
     * Preconditions: {@code snapshot} non-null; session exists.
     * Postconditions: Returns {@code revisionId}; updatedAt is refreshed.
     * Throws: IOException on write/manifest failure.
     * Notes: RevisionInfo.createdAt uses {@code snapshot.capturedAt}.
     */
    public synchronized String saveRevision(String sessionId, SessionSnapshot snapshot, String note) throws IOException {
        Objects.requireNonNull(snapshot, "snapshot");
        Path sDir = sessionDir(sessionId);
        Path revDir = sDir.resolve("revisions");
        Files.createDirectories(revDir);

        String rid = UUID.randomUUID().toString();
        Path revFile = revDir.resolve(rid + ".json");
        try (Writer w = Files.newBufferedWriter(revFile, StandardCharsets.UTF_8)) {
            gson.toJson(snapshot, w);
        }

        Manifest m = readManifest(sDir);
        m.revisions.add(new RevisionInfo(rid, snapshot.capturedAt, note == null ? "" : note)); // oldest→newest append
        m.header.updatedAt = Instant.now();
        writeManifest(sDir, m.header, m.revisions);
        return rid;
    }

    /**
     * Lists revisions in chronological order (oldest → newest).
     *
     * Does: Reads manifest and returns a sorted copy.
     * Preconditions: Session exists; may have zero revisions.
     * Postconditions: Returns immutable list view.
     * Throws: IOException on read/parse error.
     */
    public synchronized List<RevisionInfo> listRevisions(String sessionId) throws IOException {
        Manifest m = readManifest(sessionDir(sessionId));
        m.revisions.sort(Comparator.comparing(ri -> ri.createdAt));
        return List.copyOf(m.revisions);
    }

    /**
     * Loads a specific revision by id.
     *
     * Does: Reads revisions/<revisionId>.json and deserializes it.
     * Preconditions: File exists.
     * Postconditions: Returns deserialized snapshot.
     * Throws: IOException if not found or unreadable.
     */
    public synchronized SessionSnapshot loadRevision(String sessionId, String revisionId) throws IOException {
        Path file = sessionDir(sessionId).resolve("revisions").resolve(revisionId + ".json");
        if (!Files.exists(file)) throw new IOException("Revision not found: " + revisionId);
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return gson.fromJson(r, SessionSnapshot.class);
        }
    }

    /**
     * Loads the latest (most recent) revision.
     *
     * Does: Uses the last entry in manifest.revisions and loads that file.
     * Preconditions: At least one revision exists.
     * Postconditions: Returns the latest snapshot.
     * Throws: IllegalStateException if none exist; IOException on file errors.
     */
    public synchronized SessionSnapshot loadLatest(String sessionId) throws IOException {
        Manifest m = readManifest(sessionDir(sessionId));
        if (m.revisions.isEmpty()) throw new IllegalStateException("No revisions");
        RevisionInfo last = m.revisions.get(m.revisions.size() - 1);
        return loadRevision(sessionId, last.revisionId);
    }

    /**
     * Reverts the session to a given revision id:
     * - Deletes all later revision files
     * - Trims manifest to the chosen revision
     * - Updates updatedAt
     */
    public synchronized void revertToRevision(String sessionId, String revisionId) throws IOException {
        Path sDir = sessionDir(sessionId);
        Path revDir = sDir.resolve("revisions");
        Manifest m = readManifest(sDir);

        int idx = -1;
        for (int i = 0; i < m.revisions.size(); i++) {
            if (m.revisions.get(i).revisionId.equals(revisionId)) { idx = i; break; }
        }
        if (idx < 0) throw new IllegalArgumentException("Unknown revision: " + revisionId);

        for (int j = m.revisions.size() - 1; j > idx; j--) {
            Files.deleteIfExists(revDir.resolve(m.revisions.get(j).revisionId + ".json"));
            m.revisions.remove(j);
        }
        m.header.updatedAt = Instant.now();
        writeManifest(sDir, m.header, m.revisions);
    }

    // ---------- Manifest & helpers ----------

    /**
     * @return <root>/<sessionId> (never null)
     * Notes: Helper; does not create directories.
     */
    private Path sessionDir(String sessionId) {
        return root.resolve(Objects.requireNonNull(sessionId, "sessionId"));
    }

    /**
     * @return path to manifest.json within a session directory.
     */
    private Path manifestPath(Path sessionDir) {
        return sessionDir.resolve("manifest.json");
    }

    /**
     * Reads only header fields from manifest.json for fast listing.
     *
     * Does: Parses sessionId, title, createdAt, updatedAt; ignores revisions.
     * Preconditions: {@code manifest} points to a readable JSON file.
     * Postconditions: Returns a SessionInfo or null if corrupt/unreadable.
     * Notes: Swallows exceptions to keep listSessions resilient.
     */
    private SessionInfo tryReadManifestHeader(Path manifest) {
        try (Reader r = Files.newBufferedReader(manifest, StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            String sid = obj.get("sessionId").getAsString();
            String title = obj.has("title") && !obj.get("title").isJsonNull() ? obj.get("title").getAsString() : "";
            Instant createdAt = obj.has("createdAt") ? Instant.parse(obj.get("createdAt").getAsString()) : Instant.EPOCH;
            Instant updatedAt = obj.has("updatedAt") && !obj.get("updatedAt").isJsonNull()
                    ? Instant.parse(obj.get("updatedAt").getAsString()) : null;

            SessionInfo si = new SessionInfo(sid, title, createdAt);
            si.updatedAt = updatedAt;
            return si;
        } catch (Exception e) {
            return null; // skip corrupt/partial manifests
        }
    }

    /**
     * Reads full manifest (header + revisions) from disk.
     *
     * Preconditions: manifest.json exists and is readable.
     * Postconditions: Returns in-memory manifest with preserved revision order.
     * Throws: IOException on read/parse error.
     */
    private Manifest readManifest(Path sessionDir) throws IOException {
        Path mf = manifestPath(sessionDir);
        try (Reader r = Files.newBufferedReader(mf, StandardCharsets.UTF_8)) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();

            SessionInfo header = new SessionInfo(
                    obj.get("sessionId").getAsString(),
                    obj.has("title") && !obj.get("title").isJsonNull() ? obj.get("title").getAsString() : "",
                    Instant.parse(obj.get("createdAt").getAsString())
            );
            header.updatedAt = obj.has("updatedAt") && !obj.get("updatedAt").isJsonNull()
                    ? Instant.parse(obj.get("updatedAt").getAsString()) : null;

            List<RevisionInfo> revs = new ArrayList<>();
            if (obj.has("revisions") && obj.get("revisions").isJsonArray()) {
                for (JsonElement el : obj.getAsJsonArray("revisions")) {
                    JsonObject rObj = el.getAsJsonObject();
                    String rid = rObj.get("revisionId").getAsString();
                    Instant created = Instant.parse(rObj.get("createdAt").getAsString());
                    String note = rObj.has("note") && !rObj.get("note").isJsonNull() ? rObj.get("note").getAsString() : "";
                    revs.add(new RevisionInfo(rid, created, note));
                }
            }
            return new Manifest(header, revs); // preserve stored order
        }
    }

    /**
     * Writes manifest (header + revisions) to disk.
     *
     * Preconditions: session directory exists.
     * Postconditions: manifest.json reflects provided state.
     * Throws: IOException on write failure.
     * Notes: Only non-blank notes are persisted for brevity.
     */
    private void writeManifest(Path sessionDir, SessionInfo header, List<RevisionInfo> revisions) throws IOException {
        JsonObject obj = new JsonObject();
        obj.addProperty("sessionId", header.sessionId);
        obj.addProperty("title", header.title);
        obj.addProperty("createdAt", header.createdAt.toString());
        if (header.updatedAt != null) obj.addProperty("updatedAt", header.updatedAt.toString());

        JsonArray revs = new JsonArray();
        for (RevisionInfo r : revisions) {
            JsonObject rObj = new JsonObject();
            rObj.addProperty("revisionId", r.revisionId);
            rObj.addProperty("createdAt", r.createdAt.toString());
            if (r.note != null && !r.note.isBlank()) rObj.addProperty("note", r.note);
            revs.add(rObj);
        }
        obj.add("revisions", revs);

        Path mf = manifestPath(sessionDir);
        try (Writer w = Files.newBufferedWriter(mf, StandardCharsets.UTF_8)) {
            gson.toJson(JsonParser.parseString(obj.toString()), w);
        }
    }

    private static final class Manifest {
        final SessionInfo header;
        final List<RevisionInfo> revisions;
        Manifest(SessionInfo header, List<RevisionInfo> revisions) {
            this.header = header;
            this.revisions = revisions;
        }
    }

    /**
     * Gson adapter for java.time.Instant (ISO-8601).
     *
     * Notes: Keeps JSON portable and locale-agnostic.
     */
    private static final class InstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        @Override public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
        @Override public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return Instant.parse(json.getAsString());
        }
    }
}