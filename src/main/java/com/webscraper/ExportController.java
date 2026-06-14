package com.webscraper;

import com.google.gson.Gson;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;

import static spark.Spark.post;

/**
 * Server-side export of scrape results into JSON, CSV, TXT, PDF and DOCX.
 * The client POSTs the ScraperResult JSON; we generate the file in Java.
 */
public class ExportController {

    private static final Gson gson = new Gson();

    public static void register() {
        post("/api/export/json", (req, res) -> {
            ScraperResult d = parse(req.body());
            byte[] out = gson.toJson(d).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return send(res, out, "application/json", "scrape.json");
        });

        post("/api/export/csv", (req, res) -> {
            ScraperResult d = parse(req.body());
            byte[] out = buildCsv(d).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return send(res, out, "text/csv", "scrape.csv");
        });

        post("/api/export/txt", (req, res) -> {
            ScraperResult d = parse(req.body());
            byte[] out = buildPlainText(d).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return send(res, out, "text/plain", "scrape.txt");
        });

        post("/api/export/pdf", (req, res) -> {
            ScraperResult d = parse(req.body());
            return send(res, buildPdf(d), "application/pdf", "scrape-report.pdf");
        });

        post("/api/export/docx", (req, res) -> {
            ScraperResult d = parse(req.body());
            return send(res, buildDocx(d),
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "scrape-report.docx");
        });
    }

    // ── helpers ──
    private static ScraperResult parse(String body) {
        ScraperResult d = gson.fromJson(body, ScraperResult.class);
        return d != null ? d : new ScraperResult();
    }

    private static Object send(spark.Response res, byte[] bytes, String type, String filename) throws Exception {
        res.type(type);
        res.header("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        res.header("Content-Length", String.valueOf(bytes.length));
        OutputStream os = res.raw().getOutputStream();
        os.write(bytes);
        os.flush();
        return res.raw();
    }

    private static String safe(String s) { return s == null ? "" : s; }

    // ── CSV (full — every section that has data) ──
    private static String buildCsv(ScraperResult d) {
        boolean full = d.html != null && !d.html.isEmpty();
        StringBuilder sb = new StringBuilder();

        // Metadata (common to all)
        sb.append("== METADATA ==\n");
        sb.append("field,value\n");
        sb.append(csv("url")).append(',').append(csv(d.url)).append('\n');
        sb.append(csv("title")).append(',').append(csv(d.title)).append('\n');
        sb.append(csv("description")).append(',').append(csv(d.description)).append('\n');
        sb.append(csv("keywords")).append(',').append(csv(d.keywords)).append('\n');
        sb.append(csv("language")).append(',').append(csv(d.language)).append('\n');

        if (full) {
            int words = d.stats != null ? d.stats.wordCount : 0;
            sb.append("\n== SUMMARY ==\n");
            sb.append("links,images,headings,words\n");
            sb.append(d.linkCount).append(',').append(d.imageCount).append(',')
              .append(d.headingCount).append(',').append(words).append('\n');
        }

        if (has(d.links)) {
            sb.append("\n== LINKS ==\n").append("text,href,type\n");
            for (ScraperResult.LinkItem l : d.links)
                sb.append(csv(l.text)).append(',').append(csv(l.href)).append(',')
                  .append(l.external ? "external" : "internal").append('\n');
        }
        if (has(d.headings)) {
            sb.append("\n== HEADINGS ==\n").append("tag,text\n");
            for (ScraperResult.HeadingItem h : d.headings)
                sb.append(csv(h.tag)).append(',').append(csv(h.text)).append('\n');
        }
        if (has(d.images)) {
            sb.append("\n== IMAGES ==\n").append("alt,src\n");
            for (ScraperResult.ImageItem img : d.images)
                sb.append(csv(img.alt)).append(',').append(csv(img.src)).append('\n');
        }
        if (has(d.tables)) {
            int ti = 0;
            for (ScraperResult.TableItem t : d.tables) {
                ti++;
                sb.append("\n== TABLE ").append(ti).append(" ==\n");
                if (t.headers != null) {
                    for (int i = 0; i < t.headers.size(); i++) {
                        if (i > 0) sb.append(',');
                        sb.append(csv(t.headers.get(i)));
                    }
                    sb.append('\n');
                }
                for (List<String> row : t.rows) {
                    for (int i = 0; i < row.size(); i++) {
                        if (i > 0) sb.append(',');
                        sb.append(csv(row.get(i)));
                    }
                    sb.append('\n');
                }
            }
        }
        if (has(d.emails) || has(d.phones)) {
            sb.append("\n== CONTACTS ==\n").append("type,value\n");
            if (d.emails != null) for (String e : d.emails) sb.append("email,").append(csv(e)).append('\n');
            if (d.phones != null) for (String p : d.phones) sb.append("phone,").append(csv(p)).append('\n');
        }
        if (d.stats != null) {
            sb.append("\n== TEXT STATS ==\n").append("metric,value\n");
            sb.append("words,").append(d.stats.wordCount).append('\n');
            sb.append("characters,").append(d.stats.charCount).append('\n');
            sb.append("paragraphs,").append(d.stats.paragraphCount).append('\n');
            sb.append("readingTimeMin,").append(d.stats.readingTimeMin).append('\n');
        }
        return sb.toString();
    }

    private static String csv(String v) {
        return "\"" + safe(v).replace("\"", "\"\"") + "\"";
    }

    // ── Plain text (full) ──
    private static String buildPlainText(ScraperResult d) {
        StringBuilder sb = new StringBuilder();
        sb.append("WEBSCRAPER PRO — EXPORT\n");
        sb.append("URL: ").append(safe(d.url)).append('\n');
        sb.append("Title: ").append(safe(d.title)).append('\n');
        sb.append("Description: ").append(safe(d.description)).append('\n');
        sb.append("Keywords: ").append(safe(d.keywords)).append('\n');
        sb.append("Language: ").append(safe(d.language)).append('\n');

        if (has(d.links)) {
            sb.append("\nLINKS (").append(d.links.size()).append("):\n");
            for (ScraperResult.LinkItem l : d.links)
                sb.append("  - ").append(safe(l.text)).append(" -> ").append(safe(l.href))
                  .append(" [").append(l.external ? "external" : "internal").append("]\n");
        }
        if (has(d.headings)) {
            sb.append("\nHEADINGS (").append(d.headings.size()).append("):\n");
            for (ScraperResult.HeadingItem h : d.headings)
                sb.append("  ").append(h.tag).append(": ").append(safe(h.text)).append('\n');
        }
        if (has(d.images)) {
            sb.append("\nIMAGES (").append(d.images.size()).append("):\n");
            for (ScraperResult.ImageItem img : d.images)
                sb.append("  - ").append(safe(img.alt)).append(" -> ").append(safe(img.src)).append('\n');
        }
        if (has(d.tables)) {
            int ti = 0;
            for (ScraperResult.TableItem t : d.tables) {
                ti++;
                sb.append("\nTABLE ").append(ti).append(":\n");
                if (t.headers != null) sb.append("  ").append(String.join(" | ", t.headers)).append('\n');
                for (List<String> row : t.rows) sb.append("  ").append(String.join(" | ", row)).append('\n');
            }
        }
        if (has(d.emails) || has(d.phones)) {
            sb.append("\nCONTACTS:\n");
            if (d.emails != null) for (String e : d.emails) sb.append("  email: ").append(e).append('\n');
            if (d.phones != null) for (String p : d.phones) sb.append("  phone: ").append(p).append('\n');
        }
        if (d.stats != null) {
            sb.append("\nTEXT STATS: ").append(d.stats.wordCount).append(" words, ")
              .append(d.stats.charCount).append(" chars, ")
              .append(d.stats.paragraphCount).append(" paragraphs, ")
              .append(d.stats.readingTimeMin).append(" min read\n");
        }
        return sb.toString();
    }

    // ── PDF (OpenPDF) ──
    private static byte[] buildPdf(ScraperResult d) throws Exception {
        boolean full = d.html != null && !d.html.isEmpty();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document();
        PdfWriter.getInstance(doc, baos);
        doc.open();

        Font h1   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(0x33, 0x33, 0x33));
        Font h2   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(0x44, 0x44, 0x44));
        Font body = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
        Font small= FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);

        doc.add(new Paragraph("WebScraper Pro — Report", h1));
        doc.add(new Paragraph(safe(d.url), small));
        doc.add(new Paragraph(" "));

        // Metadata — common to every export
        doc.add(new Paragraph("Metadata", h2));
        doc.add(new Paragraph("Title: " + safe(d.title), body));
        doc.add(new Paragraph("Description: " + safe(d.description), body));
        doc.add(new Paragraph("Keywords: " + safe(d.keywords), body));
        doc.add(new Paragraph("Language: " + safe(d.language), body));

        // Summary — only on a full export
        if (full) {
            int words = d.stats != null ? d.stats.wordCount : 0;
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Summary", h2));
            doc.add(new Paragraph(d.linkCount + " links · " + d.imageCount + " images · "
                    + d.headingCount + " headings · " + words + " words", body));
        }

        if (has(d.headings)) {
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Headings (" + d.headings.size() + ")", h2));
            for (ScraperResult.HeadingItem h : d.headings)
                doc.add(new Paragraph(h.tag + "  " + safe(h.text), body));
        }

        if (has(d.links)) {
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Links (" + d.links.size() + ")", h2));
            PdfPTable table = new PdfPTable(new float[]{3, 5, 1.4f});
            table.setWidthPercentage(100);
            addHeaderCell(table, "Text"); addHeaderCell(table, "URL"); addHeaderCell(table, "Type");
            for (ScraperResult.LinkItem l : d.links) {
                table.addCell(new Phrase(safe(l.text), body));
                table.addCell(new Phrase(safe(l.href), body));
                table.addCell(new Phrase(l.external ? "external" : "internal", body));
            }
            doc.add(table);
        }

        if (has(d.images)) {
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Images (" + d.images.size() + ")", h2));
            for (ScraperResult.ImageItem img : d.images)
                doc.add(new Paragraph(safe(img.alt) + " — " + safe(img.src), body));
        }

        if (has(d.tables)) {
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Tables (" + d.tables.size() + ")", h2));
            int ti = 0;
            for (ScraperResult.TableItem t : d.tables) {
                ti++;
                doc.add(new Paragraph("Table " + ti + " (" + t.rowCount + " rows × " + t.colCount + " cols)", body));
                if (t.colCount > 0 && t.colCount <= 12) {
                    PdfPTable pt = new PdfPTable(t.colCount);
                    pt.setWidthPercentage(100);
                    if (t.headers != null)
                        for (String hd : t.headers) addHeaderCell(pt, hd);
                    for (List<String> row : t.rows)
                        for (String c : row) pt.addCell(new Phrase(safe(c), body));
                    doc.add(pt);
                }
            }
        }

        if (d.stats != null) {
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Text statistics", h2));
            doc.add(new Paragraph(d.stats.wordCount + " words · " + d.stats.charCount + " characters · "
                    + d.stats.paragraphCount + " paragraphs · " + d.stats.readingTimeMin + " min read", body));
        }

        if (has(d.emails) || has(d.phones)) {
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Contacts", h2));
            StringBuilder c = new StringBuilder();
            if (d.emails != null) d.emails.forEach(e -> c.append(e).append("  "));
            if (d.phones != null) d.phones.forEach(p -> c.append(p).append("  "));
            doc.add(new Paragraph(c.length() == 0 ? "None found" : c.toString(), body));
        }

        doc.close();
        return baos.toByteArray();
    }

    private static boolean has(List<?> l) { return l != null && !l.isEmpty(); }

    private static void addHeaderCell(PdfPTable table, String text) {
        Font hf = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        PdfPCell cell = new PdfPCell(new Phrase(text, hf));
        cell.setBackgroundColor(new Color(0xF0, 0xF0, 0xF0));
        cell.setPadding(5);
        table.addCell(cell);
    }

    // ── DOCX (Apache POI) ──
    private static byte[] buildDocx(ScraperResult d) throws Exception {
        boolean full = d.html != null && !d.html.isEmpty();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (XWPFDocument doc = new XWPFDocument()) {
            title(doc, "WebScraper Pro — Report");
            muted(doc, safe(d.url));

            // Metadata — common to every export
            heading(doc, "Metadata");
            para(doc, "Title: " + safe(d.title));
            para(doc, "Description: " + safe(d.description));
            para(doc, "Keywords: " + safe(d.keywords));
            para(doc, "Language: " + safe(d.language));

            if (full) {
                int words = d.stats != null ? d.stats.wordCount : 0;
                heading(doc, "Summary");
                para(doc, d.linkCount + " links · " + d.imageCount + " images · "
                        + d.headingCount + " headings · " + words + " words");
            }

            if (has(d.headings)) {
                heading(doc, "Headings (" + d.headings.size() + ")");
                for (ScraperResult.HeadingItem h : d.headings) para(doc, h.tag + "  " + safe(h.text));
            }

            if (has(d.links)) {
                heading(doc, "Links (" + d.links.size() + ")");
                for (ScraperResult.LinkItem l : d.links)
                    para(doc, "• " + safe(l.text) + " — " + safe(l.href) + " (" + (l.external ? "external" : "internal") + ")");
            }

            if (has(d.images)) {
                heading(doc, "Images (" + d.images.size() + ")");
                for (ScraperResult.ImageItem img : d.images) para(doc, "• " + safe(img.alt) + " — " + safe(img.src));
            }

            if (has(d.tables)) {
                heading(doc, "Tables (" + d.tables.size() + ")");
                int ti = 0;
                for (ScraperResult.TableItem t : d.tables) {
                    ti++;
                    para(doc, "Table " + ti + " (" + t.rowCount + " rows × " + t.colCount + " cols)");
                    if (t.headers != null) para(doc, String.join(" | ", t.headers));
                    for (List<String> row : t.rows) para(doc, String.join(" | ", row));
                }
            }

            if (d.stats != null) {
                heading(doc, "Text statistics");
                para(doc, d.stats.wordCount + " words · " + d.stats.charCount + " characters · "
                        + d.stats.paragraphCount + " paragraphs · " + d.stats.readingTimeMin + " min read");
            }

            if (has(d.emails) || has(d.phones)) {
                heading(doc, "Contacts");
                StringBuilder c = new StringBuilder();
                if (d.emails != null) d.emails.forEach(e -> c.append(e).append("   "));
                if (d.phones != null) d.phones.forEach(p -> c.append(p).append("   "));
                para(doc, c.length() == 0 ? "None found" : c.toString());
            }

            doc.write(baos);
        }
        return baos.toByteArray();
    }

    private static void title(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setBold(true); r.setFontSize(20); r.setText(text);
    }
    private static void muted(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setFontSize(9); r.setColor("888888"); r.setText(text);
    }
    private static void heading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(200);
        XWPFRun r = p.createRun();
        r.setBold(true); r.setFontSize(13); r.setText(text);
    }
    private static void para(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setFontSize(10); r.setText(text);
    }
}
