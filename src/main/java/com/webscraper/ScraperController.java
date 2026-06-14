package com.webscraper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import static spark.Spark.*;

public class ScraperController {

    private static final Gson gson = new Gson();

    public static void register() {

        // POST /api/scrape  – full page scrape (titles, links, images, meta)
        post("/api/scrape", (req, res) -> {
            res.type("application/json");
            try {
                JsonObject body = gson.fromJson(req.body(), JsonObject.class);
                if (body == null || !body.has("url")) {
                    res.status(400);
                    return gson.toJson(error("Missing 'url' field in request body"));
                }
                String url = body.get("url").getAsString().trim();
                ScraperResult result = ScraperService.scrape(url);
                return gson.toJson(new ScrapeResponse(result));
            } catch (Exception e) {
                res.status(500);
                return gson.toJson(error(friendlyError(e)));
            }
        });
    }

    private static String friendlyError(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return "Unknown error occurred.";
        if (msg.contains("Read timeout") || msg.contains("SocketTimeoutException"))
            return "Request timed out — the site may be slow or blocking scrapers.";
        if (msg.contains("Status=403"))
            return "Access denied (403) — this site blocks automated scraping.";
        if (msg.contains("Status=401"))
            return "Authentication required (401) — this page needs a login.";
        if (msg.contains("Status=404"))
            return "Page not found (404) — check the URL is correct.";
        if (msg.contains("UnknownHostException") || msg.contains("UnknownHost"))
            return "Unknown host — check the URL and your internet connection.";
        if (msg.contains("SSLHandshakeException") || msg.contains("SSL"))
            return "SSL/TLS error — could not establish a secure connection.";
        if (msg.contains("Connection refused"))
            return "Connection refused — the server at that URL is not accepting connections.";
        return "Scraping failed: " + msg;
    }

    private static JsonObject error(String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("error", message);
        return obj;
    }
}
