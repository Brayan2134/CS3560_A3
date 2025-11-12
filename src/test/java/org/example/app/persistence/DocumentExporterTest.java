package org.example.app.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DocumentExporterTest {

    private static SessionSnapshot sample() {
        return new SessionSnapshot(
                "Academic",
                "Summarize clearly",
                "Input text here.",
                "Output text here.",
                0.3,
                "formal",
                "APA",
                "summarize",
                "English",
                "gpt-4o-mini",
                512,
                Instant.parse("2025-01-01T00:00:00Z")
        );
    }

    @Test
    void exportTxt_writesExpectedContent(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("export").resolve("doc.txt");
        DocumentExporter.exportTxt(sample(), out);

        assertTrue(Files.exists(out), "TXT file should be written");
        String s = Files.readString(out);
        assertTrue(s.contains("Intelligent Writing Assistant"));
        assertTrue(s.contains("Preset: Academic"));
        assertTrue(s.contains("=== Instruction ==="));
        assertTrue(s.contains("=== Input ==="));
        assertTrue(s.contains("=== Output ==="));
        // front matter bits
        assertTrue(s.contains("model: gpt-4o-mini"));
        assertTrue(s.contains("max_tokens: 512"));
    }

    @Test
    void exportMd_writesExpectedMarkdown(@TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("export").resolve("doc.md");
        DocumentExporter.exportMd(sample(), out);

        assertTrue(Files.exists(out), "MD file should be written");
        String s = Files.readString(out);
        assertTrue(s.contains("# Intelligent Writing Assistant"));
        assertTrue(s.contains("## Instruction"));
        assertTrue(s.contains("## Input"));
        assertTrue(s.contains("## Output"));
        assertTrue(s.contains("- model: `gpt-4o-mini`"));
        assertTrue(s.contains("- temperature: `0.3`"));
    }
}
