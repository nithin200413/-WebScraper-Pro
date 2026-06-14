package com.webscraper;

import java.util.List;

public class SelectorResult {
    public String       url;
    public String       selector;
    public int          matchCount;
    public List<String> matches;

    public SelectorResult(String url, String selector, List<String> matches) {
        this.url        = url;
        this.selector   = selector;
        this.matchCount = matches.size();
        this.matches    = matches;
    }
}
