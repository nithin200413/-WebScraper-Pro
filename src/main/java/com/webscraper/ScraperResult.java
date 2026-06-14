package com.webscraper;

import java.util.List;

public class ScraperResult {

    public String url;
    public String title;
    public String description;
    public String keywords;
    public String favicon;
    public String ogImage;
    public String language;
    public String html;   // raw client-side HTML source

    public int linkCount;
    public int imageCount;
    public int headingCount;
    public int internalLinkCount;
    public int externalLinkCount;

    public List<LinkItem>    links;
    public List<ImageItem>   images;
    public List<HeadingItem> headings;
    public List<TableItem>   tables;

    public List<String> emails;
    public List<String> phones;

    public TextStats stats;

    public static class LinkItem {
        public String  text;
        public String  href;
        public boolean external;

        public LinkItem(String text, String href, boolean external) {
            this.text     = text;
            this.href     = href;
            this.external = external;
        }
    }

    public static class ImageItem {
        public String alt;
        public String src;

        public ImageItem(String alt, String src) {
            this.alt = alt;
            this.src = src;
        }
    }

    public static class HeadingItem {
        public String tag;
        public String text;

        public HeadingItem(String tag, String text) {
            this.tag  = tag;
            this.text = text;
        }
    }

    public static class TableItem {
        public List<String>       headers;
        public List<List<String>> rows;
        public int                rowCount;
        public int                colCount;

        public TableItem(List<String> headers, List<List<String>> rows) {
            this.headers  = headers;
            this.rows     = rows;
            this.rowCount = rows.size();
            this.colCount = headers != null ? headers.size()
                          : (rows.isEmpty() ? 0 : rows.get(0).size());
        }
    }

    public static class TextStats {
        public int wordCount;
        public int charCount;
        public int paragraphCount;
        public int readingTimeMin;

        public TextStats(int wordCount, int charCount, int paragraphCount, int readingTimeMin) {
            this.wordCount      = wordCount;
            this.charCount      = charCount;
            this.paragraphCount = paragraphCount;
            this.readingTimeMin = readingTimeMin;
        }
    }
}
