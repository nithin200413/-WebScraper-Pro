package com.webscraper;

/**
 * Wraps the raw scrape data together with server-rendered HTML so the
 * frontend only has to inject markup instead of building it in JavaScript.
 */
public class ScrapeResponse {
    public ScraperResult data;       // raw data (used for exports)
    public String        statsHtml;  // inner HTML for the stats bar
    public String        resultHtml; // inner HTML for the result box

    public ScrapeResponse(ScraperResult data) {
        this.data       = data;
        this.statsHtml  = ResultView.renderStats(data);
        this.resultHtml = ResultView.render(data);
    }
}
