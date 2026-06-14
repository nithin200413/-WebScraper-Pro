package com.webscraper;

import com.google.gson.JsonObject;
import com.webscraper.auth.AuthController;
import com.webscraper.db.Database;

import static spark.Spark.*;

public class Main {

    public static void main(String[] args) {
        port(8080);

        // Serve static files from resources/public
        staticFiles.location("/public");

        // ── Initialize MySQL (XAMPP) ──
        try {
            Database.init();
        } catch (Exception e) {
            System.err.println("⚠️  Database init failed: " + e.getMessage());
            System.err.println("    Make sure XAMPP MySQL is running on localhost:3306.");
        }

        // Close pool on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(Database::close));

        // CORS (same-origin, but harmless)
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type");
        });

        options("/*", (req, res) -> {
            res.status(200);
            return "OK";
        });

        // Suppress favicon.ico 404 warning
        get("/favicon.ico", (req, res) -> {
            res.status(204);
            return "";
        });

        // ── Protect scraping & export API: require an authenticated session ──
        before("/api/scrape", Main::requireAuth);
        before("/api/scrape/*", Main::requireAuth);
        before("/api/export/*", Main::requireAuth);

        // Register routes
        PageController.register();
        AuthController.register();
        ScraperController.register();
        ExportController.register();

        System.out.println("✅ Web Scraper running at http://localhost:8080");
    }

    /** Filter that blocks unauthenticated access to protected API routes. */
    private static void requireAuth(spark.Request req, spark.Response res) {
        Integer userId = req.session().attribute("userId");
        if (userId == null) {
            res.type("application/json");
            JsonObject obj = new JsonObject();
            obj.addProperty("error", "Please sign in to use the scraper.");
            halt(401, obj.toString());
        }
    }
}
