package com.webscraper.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import static spark.Spark.*;

public class AuthController {

    private static final Gson gson = new Gson();
    private static final UserDao dao = new UserDao();

    public static void register() {

        // ── POST /api/auth/signup ──
        post("/api/auth/signup", (req, res) -> {
            res.type("application/json");
            try {
                JsonObject body = gson.fromJson(req.body(), JsonObject.class);
                String name  = getStr(body, "name");
                String email = getStr(body, "email").toLowerCase();
                String pass  = getStr(body, "password");

                String validation = validate(name, email, pass);
                if (validation != null) {
                    res.status(400);
                    return gson.toJson(error(validation));
                }

                if (dao.emailExists(email)) {
                    res.status(409);
                    return gson.toJson(error("An account with this email already exists."));
                }

                User user = dao.create(name, email, PasswordUtil.hash(pass));
                req.session(true).attribute("userId", user.id);
                req.session().attribute("userName", user.name);
                req.session().attribute("userEmail", user.email);
                res.status(201);
                return gson.toJson(ok(user));
            } catch (Exception e) {
                res.status(500);
                return gson.toJson(error("Signup failed: " + e.getMessage()));
            }
        });

        // ── POST /api/auth/signin ──
        post("/api/auth/signin", (req, res) -> {
            res.type("application/json");
            try {
                JsonObject body = gson.fromJson(req.body(), JsonObject.class);
                String email = getStr(body, "email").toLowerCase();
                String pass  = getStr(body, "password");

                if (email.isEmpty() || pass.isEmpty()) {
                    res.status(400);
                    return gson.toJson(error("Email and password are required."));
                }

                User user = dao.findByEmail(email);
                if (user == null || !PasswordUtil.verify(pass, user.passwordHash)) {
                    res.status(401);
                    return gson.toJson(error("Invalid email or password."));
                }

                req.session(true).attribute("userId", user.id);
                req.session().attribute("userName", user.name);
                req.session().attribute("userEmail", user.email);
                user.passwordHash = null;
                return gson.toJson(ok(user));
            } catch (Exception e) {
                res.status(500);
                return gson.toJson(error("Sign in failed: " + e.getMessage()));
            }
        });

        // ── GET /api/auth/me ── (check current session)
        get("/api/auth/me", (req, res) -> {
            res.type("application/json");
            Integer userId = req.session().attribute("userId");
            if (userId == null) {
                res.status(401);
                return gson.toJson(error("Not authenticated"));
            }
            // Re-fetch minimal info from session-stored attributes
            JsonObject obj = new JsonObject();
            obj.addProperty("authenticated", true);
            obj.addProperty("id", userId);
            String name = req.session().attribute("userName");
            String email = req.session().attribute("userEmail");
            if (name != null)  obj.addProperty("name", name);
            if (email != null) obj.addProperty("email", email);
            return gson.toJson(obj);
        });

        // ── POST /api/auth/logout ──
        post("/api/auth/logout", (req, res) -> {
            res.type("application/json");
            req.session().invalidate();
            JsonObject obj = new JsonObject();
            obj.addProperty("success", true);
            return gson.toJson(obj);
        });
    }

    /** Returns the logged-in user id, or null. */
    public static Integer currentUserId(spark.Request req) {
        return req.session().attribute("userId");
    }

    // ── helpers ──
    private static String validate(String name, String email, String pass) {
        if (name == null || name.trim().length() < 2)
            return "Name must be at least 2 characters.";
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            return "Please enter a valid email address.";
        if (pass == null || pass.length() < 6)
            return "Password must be at least 6 characters.";
        return null;
    }

    private static String getStr(JsonObject o, String key) {
        return (o != null && o.has(key) && !o.get(key).isJsonNull())
                ? o.get(key).getAsString().trim() : "";
    }

    private static JsonObject ok(User user) {
        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        JsonObject u = new JsonObject();
        u.addProperty("id", user.id);
        u.addProperty("name", user.name);
        u.addProperty("email", user.email);
        obj.add("user", u);
        return obj;
    }

    private static JsonObject error(String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("error", message);
        return obj;
    }
}
