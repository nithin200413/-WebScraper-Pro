package com.webscraper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static spark.Spark.get;

/**
 * Serves HTML pages from /public/pages with clean, extension-less URLs
 * (e.g. /dashboard instead of /dashboard.html).
 */
public class PageController {

    public static void register() {
        serve("/",          "index.html");
        serve("/signin",    "signin.html");
        serve("/signup",    "signup.html");
        serve("/dashboard", "dashboard.html");
        serve("/terms",     "terms.html");
        serve("/privacy",   "privacy.html");
        serve("/security",  "security.html");
        serve("/status",    "status.html");
        serve("/docs",      "docs.html");
        serve("/contact",   "contact.html");
    }

    private static void serve(String route, String file) {
        get(route, (req, res) -> {
            res.type("text/html; charset=utf-8");
            try (InputStream in = PageController.class.getResourceAsStream("/public/pages/" + file)) {
                if (in == null) {
                    res.status(404);
                    return "<h1>404 — Page not found</h1>";
                }
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        });
    }
}
