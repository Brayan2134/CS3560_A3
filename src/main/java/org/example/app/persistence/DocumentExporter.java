package org.example.app.persistence;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Simple exporters for txt, md, pdf from a snapshot. */
public class DocumentExporter {

    public static void exportTxt(SessionSnapshot snap, Path out) throws Exception {
        Files.createDirectories(out.getParent());
        String body = buildPlain(snap);
        Files.writeString(out, body, StandardCharsets.UTF_8);
    }

    public static void exportMd(SessionSnapshot snap, Path out) throws Exception {
        Files.createDirectories(out.getParent());
        String body = buildMarkdown(snap);
        Files.writeString(out, body, StandardCharsets.UTF_8);
    }

    public static void exportPdf(SessionSnapshot snap, Path out) throws Exception {
        Files.createDirectories(out.getParent());
        try (FileOutputStream fos = new FileOutputStream(out.toFile())) {
            Document doc = new Document(PageSize.LETTER, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, fos);
            doc.open();

            Font h1 = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font h2 = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font mono = new Font(Font.COURIER, 10);

            doc.add(new Paragraph("Intelligent Writing Assistant", h1));
            doc.add(new Paragraph("Preset: " + snap.presetKey));
            doc.add(new Paragraph("Captured: " + snap.capturedAt.toString()));
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("Instruction", h2));
            doc.add(new Paragraph(nullToEmpty(snap.instruction)));
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("Input", h2));
            Paragraph in = new Paragraph(nullToEmpty(snap.inputText), mono);
            doc.add(in);
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("Output", h2));
            Paragraph outPara = new Paragraph(nullToEmpty(snap.outputText), mono);
            doc.add(outPara);
            doc.add(Chunk.NEWLINE);

            doc.add(new Paragraph("Config", h2));
            doc.add(new Paragraph(String.format(
                    "model=%s, maxTokens=%d, temp=%.2f, tone=%s, style=%s, textMode=%s, language=%s",
                    snap.modelId, snap.maxTokens, snap.temperature, snap.toneKey,
                    snap.grammarStyle, snap.textModeKey, snap.language)));

            doc.close();
        }
    }

    private static String buildPlain(SessionSnapshot s) {
        return """
                Intelligent Writing Assistant
                Preset: %s
                Captured: %s

                === Instruction ===
                %s

                === Input ===
                %s

                === Output ===
                %s

                === Config ===
                model=%s, maxTokens=%d, temp=%.2f, tone=%s, style=%s, textMode=%s, language=%s
                """.formatted(
                s.presetKey, s.capturedAt,
                nullToEmpty(s.instruction),
                nullToEmpty(s.inputText),
                nullToEmpty(s.outputText),
                s.modelId, s.maxTokens, s.temperature, s.toneKey, s.grammarStyle, s.textModeKey, s.language
        );
    }

    private static String buildMarkdown(SessionSnapshot s) {
        return """
                # Intelligent Writing Assistant

                **Preset:** %s  
                **Captured:** %s

                ## Instruction
                %s

                ## Input
                ```
                %s
                ```

                ## Output
                ```
                %s
                ```

                ## Config
                - model: `%s`
                - max_tokens: %d
                - temperature: %.2f
                - tone: `%s`
                - style: `%s`
                - text_mode: `%s`
                - language: `%s`
                """.formatted(
                s.presetKey, s.capturedAt,
                nullToEmpty(s.instruction),
                nullToEmpty(s.inputText),
                nullToEmpty(s.outputText),
                s.modelId, s.maxTokens, s.temperature, s.toneKey, s.grammarStyle, s.textModeKey, s.language
        );
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
}