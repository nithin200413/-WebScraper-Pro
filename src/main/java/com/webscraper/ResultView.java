package com.webscraper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Server-side HTML rendering of scrape results.
 * Produces the markup that the dashboard injects directly, keeping
 * presentation logic in Java instead of JavaScript.
 */
public class ResultView {

    /** Renders the four stat cards (inner HTML of #statsBar). */
    public static String renderStats(ScraperResult d) {
        int words = d.stats != null ? d.stats.wordCount : 0;
        StringBuilder sb = new StringBuilder();
        sb.append(statCard("s-l", iconLink(), d.linkCount, "Links"));
        sb.append(statCard("s-i", iconImage(), d.imageCount, "Images"));
        sb.append(statCard("s-h", iconHeading(), d.headingCount, "Headings"));
        sb.append(statCard("s-w", iconWords(), words, "Words"));
        return sb.toString();
    }

    /** Renders the full result box (ribbon + tabs + every panel). */
    public static String render(ScraperResult d) {
        StringBuilder sb = new StringBuilder();

        // Ribbon
        sb.append("<div class=\"ribbon glass\">");
        if (notEmpty(d.favicon))
            sb.append("<img class=\"fav\" src=\"").append(attr(d.favicon)).append("\" alt=\"\"/>");
        sb.append("<div style=\"min-width:0;flex:1\">")
          .append("<div class=\"pg-t\">").append(esc(orDash(d.title))).append("</div>")
          .append("<a class=\"pg-u\" href=\"").append(attr(d.url)).append("\" target=\"_blank\" rel=\"noopener\">")
          .append(esc(d.url)).append("</a></div></div>");

        // Tabs
        int contacts = size(d.emails) + size(d.phones);
        sb.append("<div class=\"rtabs\" role=\"tablist\">")
          .append(tab("meta", "Meta", -1, true))
          .append(tab("links", "Links", d.linkCount, false))
          .append(tab("imgs", "Images", d.imageCount, false))
          .append(tab("heads", "Headings", d.headingCount, false))
          .append(tab("tables", "Tables", size(d.tables), false))
          .append(tab("contacts", "Contacts", contacts, false))
          .append(tab("stats", "Text Stats", -1, false))
          .append(tab("shot", "Screenshot", -1, false))
          .append("</div>");

        sb.append(renderMeta(d));
        sb.append(renderLinks(d));
        sb.append(renderImages(d));
        sb.append(renderHeadings(d));
        sb.append(renderTables(d));
        sb.append(renderContacts(d));
        sb.append(renderTextStats(d));
        sb.append(renderShot(d));

        return sb.toString();
    }

    // ── panels ──
    private static String renderMeta(ScraperResult d) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div id=\"rp-meta\" class=\"rpanel\"><div class=\"mgrid\">");
        sb.append(metaItem("Title", orDash(d.title)));
        sb.append(metaItem("Description", orDash(d.description)));
        sb.append(metaItem("Keywords", orDash(d.keywords)));
        sb.append(metaItem("Language", orDash(d.language)));
        sb.append(metaItem("Favicon", orDash(d.favicon)));
        sb.append("</div>");
        if (notEmpty(d.ogImage))
            sb.append("<div class=\"og-preview\"><img src=\"").append(attr(d.ogImage))
              .append("\" alt=\"OG preview\" onerror=\"this.parentElement.style.display='none'\"/></div>");
        sb.append("</div>");
        return sb.toString();
    }

    private static String renderLinks(ScraperResult d) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div id=\"rp-links\" class=\"rpanel hidden\">");
        sb.append(toolbar("lSrch", "Filter links…", "lCnt", "links"));
        sb.append("<ul id=\"lList\" class=\"dlist\">");
        if (size(d.links) == 0) sb.append(empty("No links found"));
        else for (ScraperResult.LinkItem l : d.links) {
            String search = (safe(l.text) + " " + safe(l.href)).toLowerCase();
            sb.append("<li class=\"li\" data-search=\"").append(attr(search)).append("\">")
              .append("<div class=\"li-ico\">").append(iconLinkSmall()).append("</div>")
              .append("<div class=\"li-body\"><div class=\"li-t\">").append(esc(l.text)).append("</div>")
              .append("<div class=\"li-s\">").append(esc(l.href)).append("</div></div>")
              .append("<span class=\"li-tag ").append(l.external ? "tag-ext\">ext" : "tag-int\">int").append("</span>")
              .append("<a class=\"li-lnk\" href=\"").append(attr(l.href)).append("\" target=\"_blank\" rel=\"noopener\">\u2197</a>")
              .append("</li>");
        }
        sb.append("</ul></div>");
        return sb.toString();
    }

    private static String renderImages(ScraperResult d) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div id=\"rp-imgs\" class=\"rpanel hidden\">");
        sb.append(toolbar("iSrch", "Filter images…", "iCnt", "images"));
        sb.append("<div id=\"iGrid\" class=\"igrid\">");
        if (size(d.images) == 0) sb.append(empty("No images found"));
        else for (ScraperResult.ImageItem img : d.images) {
            String cap = "(no alt)".equals(img.alt) ? baseName(img.src) : img.alt;
            String search = (safe(img.alt) + " " + safe(img.src)).toLowerCase();
            sb.append("<div class=\"icard\" data-search=\"").append(attr(search)).append("\" tabindex=\"0\" onclick=\"window.open('")
              .append(attr(jsStr(img.src))).append("','_blank','noopener')\">")
              .append("<div class=\"ithumb-w\"><img class=\"ithumb\" loading=\"lazy\" src=\"").append(attr(img.src))
              .append("\" alt=\"").append(attr(img.alt))
              .append("\" onerror=\"this.parentElement.innerHTML='<div style=\\'display:flex;align-items:center;justify-content:center;height:100%;color:#64748b;font-size:.7rem\\'>no preview</div>'\"/></div>")
              .append("<div class=\"icap\">").append(esc(cap)).append("</div></div>");
        }
        sb.append("</div></div>");
        return sb.toString();
    }

    private static String renderHeadings(ScraperResult d) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div id=\"rp-heads\" class=\"rpanel hidden\">");
        sb.append(panelHead("Headings", "headings"));
        sb.append("<ul id=\"hList\" class=\"hlist\">");
        if (size(d.headings) == 0) sb.append(empty("No headings found"));
        else for (ScraperResult.HeadingItem h : d.headings)
            sb.append("<li class=\"hitem\" data-tag=\"").append(attr(h.tag)).append("\">")
              .append("<span class=\"htag\">").append(esc(h.tag)).append("</span>")
              .append("<span class=\"htxt\">").append(esc(h.text)).append("</span></li>");
        sb.append("</ul></div>");
        return sb.toString();
    }

    private static String renderTables(ScraperResult d) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div id=\"rp-tables\" class=\"rpanel hidden\">");
        sb.append(panelHead("Tables", "tables"));
        sb.append("<div id=\"tblWrap\" class=\"tbl-wrap\">");
        if (size(d.tables) == 0) sb.append(empty("No data tables found on this page"));
        else {
            int ti = 0;
            for (ScraperResult.TableItem t : d.tables) {
                sb.append("<div class=\"tbl-block\">")
                  .append("<div class=\"tbl-head\">")
                  .append("<span class=\"tbl-title\">Table ").append(ti + 1).append("</span>")
                  .append("<span class=\"tbl-dim\">").append(t.rowCount).append(" rows \u00d7 ")
                  .append(t.colCount).append(" cols</span>")
                  .append("<button class=\"tbl-csv\" data-tbl=\"").append(ti).append("\" data-tip=\"Download this table as CSV\">")
                  .append("<svg viewBox=\"0 0 16 16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\"><path d=\"M3 11v2a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1v-2\"/><polyline points=\"5,7 8,10 11,7\"/><line x1=\"8\" y1=\"2\" x2=\"8\" y2=\"10\"/></svg> CSV</button>")
                  .append("</div>");
                sb.append("<div class=\"tbl-scroll\"><table class=\"data-tbl\">");
                if (t.headers != null && !t.headers.isEmpty()) {
                    sb.append("<thead><tr>");
                    for (String h : t.headers) sb.append("<th>").append(esc(h)).append("</th>");
                    sb.append("</tr></thead>");
                }
                sb.append("<tbody>");
                for (List<String> row : t.rows) {
                    sb.append("<tr>");
                    for (String c : row) sb.append("<td>").append(esc(c)).append("</td>");
                    sb.append("</tr>");
                }
                sb.append("</tbody></table></div></div>");
                ti++;
            }
        }
        sb.append("</div></div>");
        return sb.toString();
    }

    private static String renderContacts(ScraperResult d) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div id=\"rp-contacts\" class=\"rpanel hidden\">");
        sb.append(panelHead("Contacts", "contacts"));
        sb.append("<ul id=\"cList\" class=\"dlist\">");
        int total = size(d.emails) + size(d.phones);
        if (total == 0) sb.append(empty("No emails or phone numbers found"));
        else {
            if (d.emails != null) for (String e : d.emails) sb.append(contactItem("email", e, iconMail()));
            if (d.phones != null) for (String p : d.phones) sb.append(contactItem("phone", p, iconPhone()));
        }
        sb.append("</ul></div>");
        return sb.toString();
    }

    private static String renderTextStats(ScraperResult d) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div id=\"rp-stats\" class=\"rpanel hidden\">");
        sb.append(panelHead("Text statistics", "stats"));
        sb.append("<div id=\"tStats\" class=\"tstats\">");
        if (d.stats == null) sb.append(empty("No text statistics"));
        else {
            sb.append(tstat(format(d.stats.wordCount), "Words"));
            sb.append(tstat(format(d.stats.charCount), "Characters"));
            sb.append(tstat(format(d.stats.paragraphCount), "Paragraphs"));
            sb.append(tstat(d.stats.readingTimeMin + " min", "Reading time"));
        }
        sb.append("</div></div>");
        return sb.toString();
    }

    private static String renderShot(ScraperResult d) {
        String shot = "https://s.wordpress.com/mshots/v1/"
                + URLEncoder.encode(safe(d.url), StandardCharsets.UTF_8) + "?w=1200";
        return "<div id=\"rp-shot\" class=\"rpanel hidden\">"
             + "<div id=\"shotWrap\" class=\"shot-wrap\" data-shot=\"" + attr(shot) + "\"></div></div>";
    }

    // ── small builders ──
    private static String statCard(String cls, String icon, int num, String label) {
        return "<div class=\"scard\"><div class=\"s-ico " + cls + "\">" + icon + "</div>"
             + "<div class=\"s-info\"><span class=\"s-num\">" + num + "</span>"
             + "<span class=\"s-lbl\">" + label + "</span></div></div>";
    }

    private static String tab(String key, String label, int badge, boolean active) {
        String b = badge >= 0 ? " <span class=\"rbadge\">" + badge + "</span>" : "";
        return "<button class=\"rtab" + (active ? " active" : "") + "\" data-rt=\"" + key + "\" role=\"tab\">"
             + label + b + "</button>";
    }

    private static String metaItem(String k, String v) {
        return "<div class=\"mitem\"><span class=\"mk\">" + esc(k) + "</span><span class=\"mv\">" + esc(v) + "</span></div>";
    }

    private static String toolbar(String inputId, String placeholder, String countId, String section) {
        return "<div class=\"toolbar\"><div class=\"srch-wrap\">"
             + "<svg class=\"srch-ico\" viewBox=\"0 0 16 16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\"><circle cx=\"7\" cy=\"7\" r=\"5\"/><line x1=\"11\" y1=\"11\" x2=\"14\" y2=\"14\"/></svg>"
             + "<input id=\"" + inputId + "\" type=\"search\" class=\"srch\" placeholder=\"" + esc(placeholder) + "\"/></div>"
             + "<span class=\"cnt\" id=\"" + countId + "\"></span>"
             + pexport(section) + "</div>";
    }

    /** Per-section export control (hover dropdown). */
    private static String pexport(String section) {
        return "<div class=\"pexport\">"
             + "<button class=\"pexport-btn\" type=\"button\" data-tip=\"Export this section\">"
             + "<svg viewBox=\"0 0 20 20\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\"><path d=\"M4 14v2a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2v-2\"/><polyline points=\"7,9 10,12 13,9\"/><line x1=\"10\" y1=\"3\" x2=\"10\" y2=\"12\"/></svg>"
             + "Export<svg viewBox=\"0 0 16 16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" style=\"width:11px;height:11px\"><path d=\"M4 6l4 4 4-4\"/></svg></button>"
             + "<div class=\"pexport-menu\">"
             + pexOpt(section, "json", "JSON")
             + pexOpt(section, "csv", "CSV")
             + pexOpt(section, "pdf", "PDF")
             + pexOpt(section, "docx", "Word")
             + "</div></div>";
    }

    private static String pexOpt(String section, String fmt, String label) {
        return "<button data-pex=\"" + section + "|" + fmt + "\">" + label
             + " <span class=\"fmt-tag\">." + fmt + "</span></button>";
    }

    /** A standalone panel header (used by panels without a search toolbar). */
    private static String panelHead(String label, String section) {
        return "<div class=\"panel-head\"><span class=\"panel-head-l\">" + esc(label) + "</span>"
             + pexport(section) + "</div>";
    }

    private static String contactItem(String type, String value, String icon) {
        return "<li class=\"li\"><div class=\"li-ico\">" + icon + "</div>"
             + "<div class=\"li-body\"><div class=\"li-t\">" + esc(value) + "</div>"
             + "<div class=\"li-s\">" + type + "</div></div>"
             + "<button class=\"copy-btn\" data-tip=\"Copy\" data-copy=\"" + attr(value) + "\">" + iconCopy() + "</button></li>";
    }

    private static String tstat(String num, String label) {
        return "<div class=\"tstat\"><div class=\"tstat-num\">" + esc(num) + "</div>"
             + "<div class=\"tstat-lbl\">" + esc(label) + "</div></div>";
    }

    private static String empty(String msg) {
        return "<div class=\"empty\"><svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\"><circle cx=\"11\" cy=\"11\" r=\"8\"/><line x1=\"21\" y1=\"21\" x2=\"16.65\" y2=\"16.65\"/></svg><span>"
             + esc(msg) + "</span></div>";
    }

    // ── icons ──
    private static String iconLink() { return "<svg viewBox=\"0 0 20 20\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\"><path d=\"M10 13a4 4 0 0 0 5.66 0l1.5-1.5a4 4 0 0 0-5.66-5.66l-.75.75\"/><path d=\"M10 7a4 4 0 0 0-5.66 0L2.84 8.5a4 4 0 0 0 5.66 5.66l.75-.75\"/></svg>"; }
    private static String iconImage() { return "<svg viewBox=\"0 0 20 20\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\"><rect x=\"2\" y=\"4\" width=\"16\" height=\"13\" rx=\"2\"/><circle cx=\"7\" cy=\"9\" r=\"1.5\"/><path d=\"M2 14l4-4 3 3 3-3 6 6\"/></svg>"; }
    private static String iconHeading() { return "<svg viewBox=\"0 0 20 20\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\"><path d=\"M4 4v12M4 10h12M16 4v12\"/></svg>"; }
    private static String iconWords() { return "<svg viewBox=\"0 0 20 20\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\"><path d=\"M3 5h14M3 10h14M3 15h9\"/></svg>"; }
    private static String iconLinkSmall() { return "<svg viewBox=\"0 0 16 16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\"><path d=\"M8 10a3 3 0 0 0 4.24 0l1-1a3 3 0 0 0-4.24-4.24l-.5.5\"/><path d=\"M8 6a3 3 0 0 0-4.24 0l-1 1a3 3 0 0 0 4.24 4.24l.5-.5\"/></svg>"; }
    private static String iconMail() { return "<svg viewBox=\"0 0 16 16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\"><rect x=\"1.5\" y=\"3\" width=\"13\" height=\"10\" rx=\"1.5\"/><path d=\"M2 4l6 4.5L14 4\"/></svg>"; }
    private static String iconPhone() { return "<svg viewBox=\"0 0 16 16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\"><path d=\"M3 3l2.5-.5 1.5 3-1.5 1a8 8 0 0 0 4 4l1-1.5 3 1.5L13 14a2 2 0 0 1-2 1A10 10 0 0 1 2 6a2 2 0 0 1 1-3z\"/></svg>"; }
    private static String iconCopy() { return "<svg viewBox=\"0 0 16 16\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.8\"><rect x=\"5\" y=\"5\" width=\"9\" height=\"9\" rx=\"1.5\"/><path d=\"M3 11V3a1 1 0 0 1 1-1h7\"/></svg>"; }

    // ── utils ──
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
    private static String attr(String s) { return esc(s); }
    private static String jsStr(String s) { return safe(s).replace("'", "\\'"); }
    private static String safe(String s) { return s == null ? "" : s; }
    private static boolean notEmpty(String s) { return s != null && !s.isEmpty(); }
    private static String orDash(String s) { return notEmpty(s) ? s : "\u2014"; }
    private static int size(List<?> l) { return l == null ? 0 : l.size(); }
    private static String format(int n) { return String.format("%,d", n); }
    private static String baseName(String url) {
        if (url == null) return "";
        int q = url.indexOf('?'); if (q >= 0) url = url.substring(0, q);
        int s = url.lastIndexOf('/');
        return s >= 0 && s < url.length() - 1 ? url.substring(s + 1) : url;
    }
}
