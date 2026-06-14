package com.webscraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScraperService {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0 Safari/537.36";

    private static final int TIMEOUT_MS = 20_000;
    private static final int MAX_ITEMS  = 5000;  // generous cap so exports include everything
    private static final int MAX_TABLES = 10;
    private static final int MAX_ROWS   = 100;
    private static final int MAX_HTML   = 3_000_000;  // ~3MB cap on raw HTML

    // Email + phone regex
    private static final Pattern EMAIL_RX =
            Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_RX =
            Pattern.compile("(?:(?:\\+\\d{1,3}[\\s.\\-]?)?(?:\\(\\d{1,4}\\)[\\s.\\-]?)?\\d{3,4}[\\s.\\-]?\\d{3,4}[\\s.\\-]?\\d{0,4})");

    /**
     * Full-page scrape: meta, links, images, headings, tables, contacts, stats.
     */
    public static ScraperResult scrape(String url) throws Exception {
        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .followRedirects(true)
                .get();

        ScraperResult result = new ScraperResult();
        result.url = url;

        // ── Basic meta ──
        result.title       = doc.title();
        result.description = metaContent(doc, "description");
        result.keywords    = metaContent(doc, "keywords");
        result.ogImage     = ogContent(doc, "og:image");
        result.language    = doc.select("html").attr("lang");

        Element faviconEl = doc.select("link[rel~=(?i)icon]").first();
        result.favicon = faviconEl != null ? faviconEl.absUrl("href") : "";

        // ── Links (deduped by href, with internal/external classification) ──
        String host = safeHost(url);
        Elements linkEls = doc.select("a[href]");
        List<ScraperResult.LinkItem> links = new ArrayList<>();
        Set<String> seenHref = new LinkedHashSet<>();
        int internal = 0, external = 0;
        for (Element el : linkEls) {
            String href = el.absUrl("href");
            if (href.isEmpty()) continue;
            if (!seenHref.add(href)) continue;          // skip duplicate URLs
            if (links.size() >= MAX_ITEMS) continue;
            boolean isExternal = isExternalLink(href, host);
            if (isExternal) external++; else internal++;
            String text = el.text().trim();
            links.add(new ScraperResult.LinkItem(
                    text.isEmpty() ? "(no text)" : text, href, isExternal));
        }
        result.links             = links;
        result.linkCount         = links.size();       // matches what is shown & exported
        result.internalLinkCount = internal;
        result.externalLinkCount = external;

        // ── Images (deduped by src) ──
        Elements imgEls = doc.select("img[src]");
        List<ScraperResult.ImageItem> images = new ArrayList<>();
        Set<String> seenSrc = new LinkedHashSet<>();
        for (Element el : imgEls) {
            String src = el.absUrl("src");
            if (src.isEmpty()) continue;
            if (!seenSrc.add(src)) continue;            // skip duplicate images
            if (images.size() >= MAX_ITEMS) continue;
            String alt = el.attr("alt").trim();
            images.add(new ScraperResult.ImageItem(alt.isEmpty() ? "(no alt)" : alt, src));
        }
        result.images     = images;
        result.imageCount = images.size();

        // ── Headings (deduped by tag + text) ──
        Elements headingEls = doc.select("h1, h2, h3, h4, h5, h6");
        List<ScraperResult.HeadingItem> headings = new ArrayList<>();
        Set<String> seenHeading = new LinkedHashSet<>();
        for (Element el : headingEls) {
            String text = el.text().trim();
            if (text.isEmpty()) continue;
            String key = el.tagName() + "|" + text;
            if (!seenHeading.add(key)) continue;        // skip duplicate headings
            if (headings.size() >= MAX_ITEMS) continue;
            headings.add(new ScraperResult.HeadingItem(el.tagName().toUpperCase(), text));
        }
        result.headings     = headings;
        result.headingCount = headings.size();

        // ── Tables ──
        result.tables = extractTables(doc);

        // ── Contacts (emails + phones) ──
        String bodyText = doc.body() != null ? doc.body().text() : doc.text();
        result.emails = extractEmails(doc, bodyText);
        result.phones = extractPhones(doc, bodyText);

        // ── Text statistics ──
        result.stats = computeStats(doc, bodyText);

        // ── Raw client-side HTML source (capped) ──
        String rawHtml = doc.outerHtml();
        result.html = rawHtml.length() > MAX_HTML ? rawHtml.substring(0, MAX_HTML) : rawHtml;

        return result;
    }

    /**
     * CSS selector scrape: return text of all matched elements.
     */
    public static SelectorResult scrapeBySelector(String url, String selector) throws Exception {
        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .followRedirects(true)
                .get();

        Elements matched = doc.select(selector);
        List<String> texts = new ArrayList<>();
        for (Element el : matched) {
            if (texts.size() >= MAX_ITEMS) break;
            String text = el.text().trim();
            if (!text.isEmpty()) texts.add(text);
        }
        return new SelectorResult(url, selector, texts);
    }

    // ════════════════ Extraction helpers ════════════════

    private static List<ScraperResult.TableItem> extractTables(Document doc) {
        List<ScraperResult.TableItem> tables = new ArrayList<>();
        for (Element table : doc.select("table")) {
            if (tables.size() >= MAX_TABLES) break;

            // Skip navigation / layout / metadata tables (e.g. Wikipedia navboxes)
            String cls = table.className().toLowerCase();
            if (cls.contains("navbox") || cls.contains("vertical-navbox")
                    || cls.contains("sidebar") || cls.contains("metadata")
                    || cls.contains("navigation") || cls.contains("infobox")
                    || "presentation".equalsIgnoreCase(table.attr("role"))) {
                continue;
            }

            // Identify the header row: prefer thead, else the first row containing <th>
            Element headerRow = table.selectFirst("thead tr");
            if (headerRow == null) {
                for (Element tr : table.select("tr")) {
                    if (!tr.select("th").isEmpty()) { headerRow = tr; break; }
                }
            }

            List<String> headers = new ArrayList<>();
            if (headerRow != null) {
                for (Element cell : headerRow.select("th, td")) {
                    headers.add(cell.text().trim());
                }
            }

            // Data rows: every <tr> except the header row, reading both td and th
            List<List<String>> rows = new ArrayList<>();
            Set<String> seenRows = new LinkedHashSet<>();
            for (Element tr : table.select("tr")) {
                if (tr == headerRow) continue;
                if (rows.size() >= MAX_ROWS) break;
                Elements cells = tr.select("td, th");
                if (cells.isEmpty()) continue;
                List<String> row = new ArrayList<>();
                for (Element c : cells) row.add(c.text().trim());
                if (row.stream().allMatch(String::isEmpty)) continue;
                if (!seenRows.add(String.join("\u0001", row))) continue;  // skip duplicate rows
                rows.add(row);
            }

            if (rows.isEmpty()) continue;

            // Normalize column count so cells align under headers
            int cols = headers.size();
            for (List<String> r : rows) cols = Math.max(cols, r.size());
            while (headers.size() < cols) headers.add("");
            for (List<String> r : rows) while (r.size() < cols) r.add("");

            // Drop columns that are entirely empty across header + all rows
            List<Integer> keep = new ArrayList<>();
            for (int c = 0; c < cols; c++) {
                boolean any = !headers.get(c).isEmpty();
                if (!any) for (List<String> r : rows) if (!r.get(c).isEmpty()) { any = true; break; }
                if (any) keep.add(c);
            }
            if (keep.isEmpty() || keep.size() > 20) continue;  // skip degenerate/layout tables

            boolean hasHeader = headers.stream().anyMatch(h -> !h.isEmpty());
            List<String> newHeaders = new ArrayList<>();
            for (int c : keep) newHeaders.add(headers.get(c));
            List<List<String>> newRows = new ArrayList<>();
            for (List<String> r : rows) {
                List<String> nr = new ArrayList<>();
                for (int c : keep) nr.add(r.get(c));
                newRows.add(nr);
            }

            tables.add(new ScraperResult.TableItem(hasHeader ? newHeaders : null, newRows));
        }
        return tables;
    }

    private static List<String> extractEmails(Document doc, String text) {
        Set<String> set = new LinkedHashSet<>();
        // mailto: links first
        for (Element a : doc.select("a[href^=mailto:]")) {
            String mail = a.attr("href").substring(7).split("\\?")[0].trim();
            if (!mail.isEmpty()) set.add(mail);
        }
        Matcher m = EMAIL_RX.matcher(text);
        while (m.find() && set.size() < MAX_ITEMS) set.add(m.group());
        return new ArrayList<>(set);
    }

    private static List<String> extractPhones(Document doc, String text) {
        Set<String> set = new LinkedHashSet<>();
        for (Element a : doc.select("a[href^=tel:]")) {
            String tel = a.attr("href").substring(4).trim();
            if (!tel.isEmpty()) set.add(tel);
        }
        Matcher m = PHONE_RX.matcher(text);
        while (m.find() && set.size() < MAX_ITEMS) {
            String p = m.group().trim();
            // Filter out short/noise matches – require at least 7 digits
            long digits = p.chars().filter(Character::isDigit).count();
            if (digits >= 7 && digits <= 15) set.add(p);
        }
        return new ArrayList<>(set);
    }

    private static ScraperResult.TextStats computeStats(Document doc, String bodyText) {
        int chars = bodyText.length();
        int words = bodyText.trim().isEmpty() ? 0 : bodyText.trim().split("\\s+").length;
        int paragraphs = doc.select("p").size();
        int readingMin = Math.max(1, (int) Math.ceil(words / 200.0));  // ~200 wpm
        return new ScraperResult.TextStats(words, chars, paragraphs, readingMin);
    }

    // ════════════════ Utility ════════════════

    private static String metaContent(Document doc, String name) {
        Element el = doc.select("meta[name=" + name + "]").first();
        if (el == null) el = doc.select("meta[property=og:" + name + "]").first();
        return el != null ? el.attr("content") : "";
    }

    private static String ogContent(Document doc, String property) {
        Element el = doc.select("meta[property=" + property + "]").first();
        return el != null ? el.absUrl("content").isEmpty()
                ? el.attr("content") : el.absUrl("content") : "";
    }

    private static String safeHost(String url) {
        try { return new URI(url).getHost(); }
        catch (Exception e) { return ""; }
    }

    private static boolean isExternalLink(String href, String host) {
        if (host == null || host.isEmpty()) return false;
        try {
            String linkHost = new URI(href).getHost();
            return linkHost != null && !linkHost.equalsIgnoreCase(host);
        } catch (Exception e) {
            return false;
        }
    }
}
